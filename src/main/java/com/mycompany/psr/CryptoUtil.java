package com.mycompany.psr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil {

    private static final String ALGORITMO = "SHA-256";
    private static final String SEPARADOR = "|";


    public static String hashSHA256(String datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO);
            byte[] hashBytes = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }


    public static String crearMensajeFirmado(String contenido) {
        String hash = hashSHA256(contenido);
        return contenido + SEPARADOR + hash;
    }

    public static String verificarMensajeFirmado(String mensajeFirmado) {
        int ultimoSeparador = mensajeFirmado.lastIndexOf(SEPARADOR);
        if (ultimoSeparador < 0) {
            return null;
        }
        String contenido = mensajeFirmado.substring(0, ultimoSeparador);
        String hashRecibido = mensajeFirmado.substring(ultimoSeparador + 1);
        String hashCalculado = hashSHA256(contenido);

        if (hashCalculado.equals(hashRecibido)) {
            return contenido;
        }
        return null;
    }
}
