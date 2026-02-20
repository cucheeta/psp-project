package com.mycompany.psr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Cliente SSL de Piedra-Papel-Tijera.
 *
 * - SSLSocket: establece canal cifrado con TLS al servidor.
 * - Truststore JKS: contiene el certificado del servidor para validarlo.
 * - SHA-256 (via CryptoUtil): firma mensajes enviados y verifica los recibidos.
 */
public class Cliente {

    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;
    private static final String TRUSTSTORE_RESOURCE = "/keystore.jks";
    private static final String TRUSTSTORE_PASSWORD = "psr12345";

    private DataInputStream in;
    private DataOutputStream out;

    public Cliente() {
        try {
            SSLSocket socket = crearSocketSSL();
            socket.startHandshake(); // Completa el handshake TLS explícitamente
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Conectado al servidor con SSL/TLS — protocolo: " + socket.getSession().getProtocol());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error al conectar al servidor SSL");
        }
    }

    /**
     * Crea un SSLSocket que confía en el certificado del servidor
     * cargando el truststore desde los recursos del proyecto.
     */
    private SSLSocket crearSocketSSL() throws Exception {
        // 1. Cargar el truststore (mismo keystore que el servidor, contiene su certificado)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream ts = getClass().getResourceAsStream(TRUSTSTORE_RESOURCE)) {
            if (ts == null) {
                throw new RuntimeException("Truststore no encontrado en " + TRUSTSTORE_RESOURCE);
            }
            trustStore.load(ts, TRUSTSTORE_PASSWORD.toCharArray());
        }

        // 2. Inicializar TrustManagerFactory con el truststore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 3. Crear el contexto SSL con TLS
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // 4. Crear y retornar el SSLSocket
        SSLSocketFactory factory = sslContext.getSocketFactory();
        return (SSLSocket) factory.createSocket(HOST, PUERTO);
    }

    /**
     * Envía un dato firmado con hash SHA-256 al servidor.
     *
     * @param dato mensaje a enviar
     */
    public void enviarDato(String dato) {
        try {
            out.writeUTF(CryptoUtil.crearMensajeFirmado(dato));
        } catch (IOException ex) {
            System.err.println("Error al enviar el dato: " + ex.getMessage());
        }
    }

    /**
     * Recibe un mensaje del servidor y verifica su integridad SHA-256.
     *
     * @return contenido del mensaje si el hash es válido
     * @throws IOException si hay error de comunicación
     */
    public String recibirDato() throws IOException {
        String mensajeFirmado = in.readUTF();
        String contenido = CryptoUtil.verificarMensajeFirmado(mensajeFirmado);
        if (contenido == null) {
            System.err.println("ADVERTENCIA: mensaje con hash inválido recibido del servidor.");
            // Extraer contenido de igual modo para no interrumpir la partida
            int sep = mensajeFirmado.lastIndexOf("|");
            return sep > 0 ? mensajeFirmado.substring(0, sep) : mensajeFirmado;
        }
        return contenido;
    }
}
