package com.mycompany.psr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor {

    public static void main(String[] args) {

        ServerSocket servidor = null;
        Socket jugador1 = null;
        Socket jugador2 = null;
        DataInputStream in1, in2;
        DataOutputStream out1, out2;

        final int PUERTO = 5000;

        try {
            servidor = new ServerSocket(PUERTO);
            System.out.println("Servidor de Piedra-Papel-Tijera iniciado en el puerto " + PUERTO);

            while (true) {

                // Esperar al jugador 1
                System.out.println("\nEsperando al jugador 1...");
                jugador1 = servidor.accept();
                out1 = new DataOutputStream(jugador1.getOutputStream());
                in1 = new DataInputStream(jugador1.getInputStream());
                out1.writeUTF("Eres el Jugador 1. Esperando al otro jugador...");
                System.out.println("Jugador 1 conectado");

                // Esperar al jugador 2
                System.out.println("Esperando al jugador 2...");
                jugador2 = servidor.accept();
                out2 = new DataOutputStream(jugador2.getOutputStream());
                in2 = new DataInputStream(jugador2.getInputStream());
                out2.writeUTF("Eres el Jugador 2. Ambos jugadores conectados!");
                System.out.println("Jugador 2 conectado");

                // Avisar al jugador 1 que ya estan los dos
                out1.writeUTF("Ambos jugadores conectados!");

                int ronda = 1;
                boolean seguirJugando = true;

                while (seguirJugando) {

                    System.out.println("\n--- Ronda " + ronda + " ---");

                    // Pedir eleccion a ambos jugadores
                    out1.writeUTF("ELEGIR");
                    out2.writeUTF("ELEGIR");

                    // Recibir elecciones
                    String eleccion1 = in1.readUTF().toLowerCase().trim();
                    String eleccion2 = in2.readUTF().toLowerCase().trim();

                    System.out.println("Jugador 1 eligio: " + eleccion1);
                    System.out.println("Jugador 2 eligio: " + eleccion2);

                    // Determinar el resultado
                    String resultado = determinarGanador(eleccion1, eleccion2);
                    System.out.println("Resultado: " + resultado);

                    // Enviar resultado a ambos jugadores
                    out1.writeUTF("Tu: " + eleccion1 + " | Rival: " + eleccion2 + " | " + resultado);
                    out2.writeUTF("Tu: " + eleccion2 + " | Rival: " + eleccion1 + " | " + invertirResultado(resultado));

                    // Preguntar si quieren seguir jugando
                    out1.writeUTF("CONTINUAR");
                    out2.writeUTF("CONTINUAR");

                    String respuesta1 = in1.readUTF().toLowerCase().trim();
                    String respuesta2 = in2.readUTF().toLowerCase().trim();

                    if (respuesta1.equals("no") || respuesta2.equals("no")) {
                        seguirJugando = false;
                        out1.writeUTF("FIN");
                        out2.writeUTF("FIN");
                    } else {
                        out1.writeUTF("SIGUIENTE");
                        out2.writeUTF("SIGUIENTE");
                    }

                    ronda++;
                }

                // Cerrar conexiones de ambos jugadores
                jugador1.close();
                jugador2.close();
                System.out.println("Partida finalizada. Jugadores desconectados.");
            }

        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String determinarGanador(String e1, String e2) {
        if (e1.equals(e2)) {
            return "Empate!";
        }

        if ((e1.equals("piedra") && e2.equals("tijera"))
                || (e1.equals("tijera") && e2.equals("papel"))
                || (e1.equals("papel") && e2.equals("piedra"))) {
            return "Gana Jugador 1!";
        }

        return "Gana Jugador 2!";
    }

    private static String invertirResultado(String resultado) {
        if (resultado.equals("Gana Jugador 1!")) {
            return "Gana tu rival!";
        } else if (resultado.equals("Gana Jugador 2!")) {
            return "Has ganado!";
        }
        return resultado;
    }
}
