package com.mycompany.psr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLSocket;

/**
 * Hilo que gestiona una partida completa de Piedra-Papel-Tijera
 * entre dos jugadores conectados mediante SSLSocket.
 *
 * Cada instancia corre en su propio Thread, permitiendo al servidor
 * manejar múltiples partidas simultáneas de forma concurrente.
 *
 * Los mensajes se firman y verifican con SHA-256 (via CryptoUtil)
 * para garantizar la integridad de la comunicación.
 */
public class HiloJuego implements Runnable {

    private final SSLSocket socketJ1;
    private final SSLSocket socketJ2;
    private final int numeroPartida;

    public HiloJuego(SSLSocket socketJ1, SSLSocket socketJ2, int numeroPartida) {
        this.socketJ1 = socketJ1;
        this.socketJ2 = socketJ2;
        this.numeroPartida = numeroPartida;
    }

    @Override
    public void run() {
        String hiloNombre = Thread.currentThread().getName();
        System.out.println("[" + hiloNombre + "] Partida " + numeroPartida + " iniciada.");

        try {
            DataInputStream in1 = new DataInputStream(socketJ1.getInputStream());
            DataInputStream in2 = new DataInputStream(socketJ2.getInputStream());
            DataOutputStream out1 = new DataOutputStream(socketJ1.getOutputStream());
            DataOutputStream out2 = new DataOutputStream(socketJ2.getOutputStream());

            // Mensajes de bienvenida
            enviar(out1, "Eres el Jugador 1. Esperando al otro jugador...");
            enviar(out2, "Eres el Jugador 2. Esperando al otro jugador...");
            enviar(out1, "Ambos jugadores conectados!");
            enviar(out2, "Ambos jugadores conectados!");

            int contadorJ1 = 0;
            int contadorJ2 = 0;
            int ronda = 1;
            boolean seguirJugando = true;

            while (seguirJugando) {
                System.out.println("[" + hiloNombre + "] --- Ronda " + ronda + " ---");

                // Solicitar elección a ambos jugadores
                enviar(out1, "ELEGIR");
                enviar(out2, "ELEGIR");

                // Recibir y verificar integridad de las elecciones
                String eleccion1 = recibirYVerificar(in1, "Jugador 1");
                String eleccion2 = recibirYVerificar(in2, "Jugador 2");

                if (eleccion1 == null || eleccion2 == null) {
                    System.out.println("[" + hiloNombre + "] ALERTA: Hash inválido detectado. Terminando partida.");
                    enviar(out1, "ERROR: Integridad comprometida. Fin de partida.");
                    enviar(out2, "ERROR: Integridad comprometida. Fin de partida.");
                    break;
                }

                eleccion1 = eleccion1.toLowerCase().trim();
                eleccion2 = eleccion2.toLowerCase().trim();
                System.out.println("[" + hiloNombre + "] J1=" + eleccion1 + "  J2=" + eleccion2);

                // Determinar ganador y actualizar marcador
                String resultado = determinarGanador(eleccion1, eleccion2);
                if (resultado.equals("Gana Jugador 1!")) {
                    contadorJ1++;
                } else if (resultado.equals("Gana Jugador 2!")) {
                    contadorJ2++;
                }

                String marcador = "Marcador:" + contadorJ1 + "-" + contadorJ2;
                System.out.println("[" + hiloNombre + "] " + resultado + "  " + marcador);

                // Enviar resultado a cada jugador con perspectiva propia
                enviar(out1, "Tu: " + eleccion1 + "  Rival: " + eleccion2 + " -- " + resultado + "  " + marcador);
                enviar(out2, "Tu: " + eleccion2 + "  Rival: " + eleccion1 + " -- " + invertirResultado(resultado) + "  " + marcador);
                enviar(out1, marcador);
                enviar(out2, "Marcador:" + contadorJ2 + "-" + contadorJ1);

                // Preguntar si continuar
                enviar(out1, "CONTINUAR");
                enviar(out2, "CONTINUAR");

                String respuesta1 = recibirYVerificar(in1, "Jugador 1");
                String respuesta2 = recibirYVerificar(in2, "Jugador 2");

                if (respuesta1 == null) respuesta1 = "no";
                if (respuesta2 == null) respuesta2 = "no";

                if (respuesta1.equalsIgnoreCase("no") || respuesta2.equalsIgnoreCase("no")) {
                    seguirJugando = false;
                    enviar(out1, "FIN");
                    enviar(out2, "FIN");
                } else {
                    enviar(out1, "SIGUIENTE");
                    enviar(out2, "SIGUIENTE");
                }

                ronda++;
            }

        } catch (IOException ex) {
            System.out.println("[" + hiloNombre + "] Error de comunicación: " + ex.getMessage());
        } finally {
            cerrarSocket(socketJ1, "Jugador 1");
            cerrarSocket(socketJ2, "Jugador 2");
            System.out.println("[" + hiloNombre + "] Partida " + numeroPartida + " finalizada.");
        }
    }

    /**
     * Envía un mensaje firmado con hash SHA-256 al cliente.
     */
    private void enviar(DataOutputStream out, String mensaje) throws IOException {
        out.writeUTF(CryptoUtil.crearMensajeFirmado(mensaje));
    }

    /**
     * Recibe un mensaje y verifica su hash SHA-256.
     * Retorna el contenido si es válido, null si está comprometido.
     */
    private String recibirYVerificar(DataInputStream in, String origen) throws IOException {
        String mensajeFirmado = in.readUTF();
        String contenido = CryptoUtil.verificarMensajeFirmado(mensajeFirmado);
        if (contenido == null) {
            System.out.println("[Partida-" + numeroPartida + "] ADVERTENCIA: hash inválido de " + origen);
        }
        return contenido;
    }

    private void cerrarSocket(SSLSocket socket, String nombre) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar socket de " + nombre + ": " + e.getMessage());
        }
    }

    // ---- Lógica del juego ----

    private static String determinarGanador(String e1, String e2) {
        if (e1.equals(e2)) return "Empate!";
        if ((e1.equals("piedra") && e2.equals("tijera"))
                || (e1.equals("tijera") && e2.equals("papel"))
                || (e1.equals("papel") && e2.equals("piedra"))) {
            return "Gana Jugador 1!";
        }
        return "Gana Jugador 2!";
    }

    private static String invertirResultado(String resultado) {
        return switch (resultado) {
            case "Gana Jugador 1!" -> "Gana tu rival!";
            case "Gana Jugador 2!" -> "Has ganado!";
            default -> resultado;
        };
    }
}
