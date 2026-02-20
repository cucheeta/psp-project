package com.mycompany.psr;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final String RUTA_KS = "/keystore.jks";
    private static final String PASS_KS = "psr12345";

    private static final AtomicInteger contadorPartidas = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println(">>> Servidor PSR Iniciado (SSL/TLS)");

        try {
            SSLServerSocket socketServidor = inicializarSSL();
            System.out.println("Escuchando en el puerto: " + PUERTO);

            while (true) {
                System.out.println("Esperando primer cliente...");
                SSLSocket j1 = (SSLSocket) socketServidor.accept();
                System.out.println("J1 conectado desde: " + j1.getInetAddress());

                System.out.println("Esperando segundo cliente...");
                SSLSocket j2 = (SSLSocket) socketServidor.accept();
                System.out.println("J2 conectado desde: " + j2.getInetAddress());

                int idPartida = contadorPartidas.incrementAndGet();
                
                HiloJuego gestor = new HiloJuego(j1, j2, idPartida);
                Thread hiloPartida = new Thread(gestor, "Hilo-Partida-" + idPartida);
                hiloPartida.start();

                System.out.println("Partida [" + idPartida + "] lanzada correctamente.\n");
            }

        } catch (Exception e) {
            System.err.println("Error en el core del servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static SSLServerSocket inicializarSSL() throws Exception {
        KeyStore almacen = KeyStore.getInstance("JKS");
        try (InputStream input = Servidor.class.getResourceAsStream(RUTA_KS)) {
            if (input == null) {
                throw new RuntimeException("Fichero JKS no encontrado en resources");
            }
            almacen.load(input, PASS_KS.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(almacen, PASS_KS.toCharArray());

        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory fabrica = contexto.getServerSocketFactory();
        return (SSLServerSocket) fabrica.createServerSocket(PUERTO);
    }
}