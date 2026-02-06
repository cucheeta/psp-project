package com.mycompany.psr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cliente {

    public static void main(String[] args) {

        final String HOST = "127.0.0.1";
        final int PUERTO = 5000;
        DataInputStream in;
        DataOutputStream out;

        try {
            Socket sc = new Socket(HOST, PUERTO);
            in = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());
            Scanner teclado = new Scanner(System.in);

            // Recibir mensaje de bienvenida
            System.out.println(in.readUTF());

            // Recibir mensaje de que ambos estan conectados
            System.out.println(in.readUTF());

            boolean seguirJugando = true;

            while (seguirJugando) {

                // Recibir orden del servidor
                String orden = in.readUTF();

                if (orden.equals("ELEGIR")) {
                    System.out.println("\nElige: piedra, papel o tijera");

                    // Leer y validar eleccion del usuario
                    String eleccion;
                    do {
                        System.out.print(">> ");
                        eleccion = teclado.nextLine().toLowerCase().trim();
                    } while (!eleccion.equals("piedra") && !eleccion.equals("papel") && !eleccion.equals("tijera"));

                    // Enviar eleccion al servidor
                    out.writeUTF(eleccion);
                    System.out.println("Esperando al otro jugador...");

                    // Recibir resultado
                    System.out.println(in.readUTF());

                } else if (orden.equals("CONTINUAR")) {
                    System.out.println("\nQuieres seguir jugando? (si/no)");

                    // Leer y validar respuesta
                    String respuesta;
                    do {
                        System.out.print(">> ");
                        respuesta = teclado.nextLine().toLowerCase().trim();
                    } while (!respuesta.equals("si") && !respuesta.equals("no"));

                    // Enviar respuesta al servidor
                    out.writeUTF(respuesta);

                } else if (orden.equals("FIN")) {
                    System.out.println("\nFin de la partida. Gracias por jugar!");
                    seguirJugando = false;

                } else if (orden.equals("SIGUIENTE")) {
                    System.out.println("\n--- Nueva ronda ---");
                }
            }

            sc.close();

        } catch (IOException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
