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

public class Cliente {

    private static final String HOST = "10.6.4.85";
    private static final int PUERTO = 5000;
    private static final String TRUSTSTORE_RESOURCE = "/keystore.jks";
    private static final String TRUSTSTORE_PASSWORD = "psr12345";

    private DataInputStream in;
    private DataOutputStream out;

    public Cliente() {
        try {
            SSLSocket socket = crearSocketSSL();
            socket.startHandshake();
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Conectado al servidor con SSL/TLS — protocolo: " + socket.getSession().getProtocol());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error al conectar al servidor SSL");
        }
    }

    private SSLSocket crearSocketSSL() throws Exception {
       
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream ts = getClass().getResourceAsStream(TRUSTSTORE_RESOURCE)) {
            if (ts == null) {
                throw new RuntimeException("Truststore no encontrado en " + TRUSTSTORE_RESOURCE);
            }
            trustStore.load(ts, TRUSTSTORE_PASSWORD.toCharArray());
        }

        // inicializamos TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        return (SSLSocket) factory.createSocket(HOST, PUERTO);
    }

    public void enviarDato(String dato) {
        try {
            out.writeUTF(CryptoUtil.crearMensajeFirmado(dato));
        } catch (IOException ex) {
            System.err.println("Error al enviar el dato: " + ex.getMessage());
        }
    }

    public String recibirDato() throws IOException {
        String mensajeFirmado = in.readUTF();
        String contenido = CryptoUtil.verificarMensajeFirmado(mensajeFirmado);
        if (contenido == null) {
            System.err.println("ADVERTENCIA: mensaje con hash inválido recibido del servidor.");
            // 
            int sep = mensajeFirmado.lastIndexOf("|");
            return sep > 0 ? mensajeFirmado.substring(0, sep) : mensajeFirmado;
        }
        return contenido;
    }
}
