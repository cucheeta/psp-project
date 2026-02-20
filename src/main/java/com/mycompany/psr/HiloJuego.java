package com.mycompany.psr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLSocket;

public class HiloJuego implements Runnable {

    private final SSLSocket cliente1;
    private final SSLSocket cliente2;
    private final int idPartida;

    public HiloJuego(SSLSocket s1, SSLSocket s2, int num) {
        this.cliente1 = s1;
        this.cliente2 = s2;
        this.idPartida = num;
    }

    @Override
    public void run() {
        String logTag = "PARTIDA-" + idPartida;
        System.out.println("[" + logTag + "] Hilo de gestión iniciado.");

        try {
            DataInputStream flujoIn1 = new DataInputStream(cliente1.getInputStream());
            DataInputStream flujoIn2 = new DataInputStream(cliente2.getInputStream());
            DataOutputStream flujoOut1 = new DataOutputStream(cliente1.getOutputStream());
            DataOutputStream flujoOut2 = new DataOutputStream(cliente2.getOutputStream());

            enviar(flujoOut1, "Eres el Jugador 1. Esperando...");
            enviar(flujoOut2, "Eres el Jugador 2. Esperando...");
            enviar(flujoOut1, "¡Comenzamos!");
            enviar(flujoOut2, "¡Comenzamos!");

            int puntosJ1 = 0;
            int puntosJ2 = 0;
            int rondaActual = 1;
            boolean activo = true;

            while (activo) {
                System.out.println("[" + logTag + "] Ronda: " + rondaActual);

                enviar(flujoOut1, "ELEGIR");
                enviar(flujoOut2, "ELEGIR");

                String jugada1 = validarEntrada(flujoIn1, "J1");
                String jugada2 = validarEntrada(flujoIn2, "J2");

                if (jugada1 == null || jugada2 == null) {
                    System.err.println("[" + logTag + "] Error de integridad en los datos.");
                    enviar(flujoOut1, "ERROR: Hash no válido.");
                    enviar(flujoOut2, "ERROR: Hash no válido.");
                    break;
                }

                jugada1 = jugada1.toLowerCase().trim();
                jugada2 = jugada2.toLowerCase().trim();

                String res = evaluar(jugada1, jugada2);
                
                if (res.contains("Jugador 1")) puntosJ1++;
                else if (res.contains("Jugador 2")) puntosJ2++;

                String marcadorInfo = "Marcador: " + puntosJ1 + " - " + puntosJ2;
                
                enviar(flujoOut1, "Tú: " + jugada1 + " | Rival: " + jugada2 + " -> " + res);
                enviar(flujoOut2, "Tú: " + jugada2 + " | Rival: " + jugada1 + " -> " + traducir(res));
                
                enviar(flujoOut1, marcadorInfo);
                enviar(flujoOut2, "Marcador: " + puntosJ2 + " - " + puntosJ1);

                enviar(flujoOut1, "CONTINUAR");
                enviar(flujoOut2, "CONTINUAR");

                String r1 = validarEntrada(flujoIn1, "J1");
                String r2 = validarEntrada(flujoIn2, "J2");

                if ("no".equalsIgnoreCase(r1) || "no".equalsIgnoreCase(r2)) {
                    activo = false;
                    enviar(flujoOut1, "FIN");
                    enviar(flujoOut2, "FIN");
                } else {
                    enviar(flujoOut1, "SIGUIENTE");
                    enviar(flujoOut2, "SIGUIENTE");
                }
                rondaActual++;
            }

        } catch (IOException e) {
            System.err.println("[" + logTag + "] Excepción de E/S: " + e.getMessage());
        } finally {
            desconectar(cliente1);
            desconectar(cliente2);
        }
    }

    private void enviar(DataOutputStream dos, String msg) throws IOException {
        dos.writeUTF(CryptoUtil.crearMensajeFirmado(msg));
    }

    private String validarEntrada(DataInputStream dis, String id) throws IOException {
        String data = dis.readUTF();
        String verificado = CryptoUtil.verificarMensajeFirmado(data);
        if (verificado == null) {
            System.out.println("Warning: Hash corrupto desde " + id);
        }
        return verificado;
    }

    private void desconectar(SSLSocket s) {
        try {
            if (s != null && !s.isClosed()) s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String evaluar(String p1, String p2) {
        if (p1.equals(p2)) return "Empate!";
        if ((p1.equals("piedra") && p2.equals("tijera")) || 
            (p1.equals("tijera") && p2.equals("papel")) || 
            (p1.equals("papel") && p2.equals("piedra"))) {
            return "Gana Jugador 1!";
        }
        return "Gana Jugador 2!";
    }

    private static String traducir(String res) {
        return switch (res) {
            case "Gana Jugador 1!" -> "Gana tu rival!";
            case "Gana Jugador 2!" -> "¡Has ganado!";
            default -> res;
        };
    }
}