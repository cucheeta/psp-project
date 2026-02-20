package com.mycompany.psr;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * Servidor SSL de Piedra-Papel-Tijera con soporte de hilos concurrentes.
 *
 * - SSLServerSocket: cifra toda la comunicación con TLS.
 * - Keystore JKS: contiene el certificado autofirmado del servidor.
 * - Thread por partida: cada par de jugadores corre en su propio HiloJuego.
 * - SHA-256 (via CryptoUtil): verifica integridad de cada mensaje.
 */
public class Servidor {

    private static final int PUERTO = 5000;
    private static final String KEYSTORE_RESOURCE = "/keystore.jks";
    private static final String KEYSTORE_PASSWORD = "psr12345";

    // Contador atómico seguro para múltiples hilos
    private static final AtomicInteger contadorPartidas = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=== Servidor PSR — SSL + Hilos + SHA-256 ===");

        try {
            SSLServerSocket servidor = crearServidorSSL();
            System.out.println("Servidor SSL iniciado en el puerto " + PUERTO);
            System.out.println("Cifrado: TLS  |  Integridad: SHA-256");
            System.out.println("Esperando conexiones seguras...\n");

            while (true) {
                // Aceptar dos jugadores para una partida
                System.out.println("Esperando Jugador 1...");
                SSLSocket jugador1 = (SSLSocket) servidor.accept();
                System.out.println("Jugador 1 conectado: " + jugador1.getInetAddress() + " (TLS activo)");

                System.out.println("Esperando Jugador 2...");
                SSLSocket jugador2 = (SSLSocket) servidor.accept();
                System.out.println("Jugador 2 conectado: " + jugador2.getInetAddress() + " (TLS activo)");

                // Crear y lanzar el hilo de la partida
                int numPartida = contadorPartidas.incrementAndGet();
                HiloJuego hilo = new HiloJuego(jugador1, jugador2, numPartida);
                Thread t = new Thread(hilo, "Partida-" + numPartida);
                t.setDaemon(false);
                t.start();

                System.out.println("Partida " + numPartida + " iniciada en hilo [" + t.getName() + "]\n");
            }

        } catch (Exception ex) {
            System.err.println("Error fatal en el servidor: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Crea un SSLServerSocket cargando el keystore desde los recursos del proyecto.
     * El keystore contiene el par clave privada / certificado autofirmado.
     */
    private static SSLServerSocket crearServidorSSL() throws Exception {
        // 1. Cargar el keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream ks = Servidor.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (ks == null) {
                throw new RuntimeException("Keystore no encontrado en " + KEYSTORE_RESOURCE
                        + "\nEjecuta: keytool -genkeypair -alias psr-server -keyalg RSA "
                        + "-keystore src/main/resources/keystore.jks -storepass psr12345 -validity 365");
            }
            keyStore.load(ks, KEYSTORE_PASSWORD.toCharArray());
        }

        // 2. Inicializar KeyManagerFactory con el keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        // 3. Crear el contexto SSL con TLS
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        // 4. Crear y retornar el SSLServerSocket
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        return (SSLServerSocket) factory.createServerSocket(PUERTO);
    }
}
