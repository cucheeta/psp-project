package com.mycompany.psr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad criptográfica que aplica hashing SHA-256 para garantizar
 * la integridad de los mensajes intercambiados entre cliente y servidor.
 *
 * Formato del mensaje firmado: "contenido|hashSHA256"
 */
public class CryptoUtil {

    private static final String ALGORITMO = "SHA-256";
    private static final String SEPARADOR = "|";

    /**
     * Calcula el hash SHA-256 de una cadena de texto.
     *
     * @param datos texto a hashear
     * @return hash en formato hexadecimal (64 caracteres)
     */
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

    /**
     * Crea un mensaje firmado con su hash SHA-256.
     * Formato: "contenido|hash"
     *
     * @param contenido mensaje original
     * @return mensaje firmado
     */
    public static String crearMensajeFirmado(String contenido) {
        String hash = hashSHA256(contenido);
        return contenido + SEPARADOR + hash;
    }

    /**
     * Verifica la integridad de un mensaje firmado y extrae su contenido.
     * Si el hash no coincide, retorna null (mensaje comprometido).
     *
     * @param mensajeFirmado mensaje con formato "contenido|hash"
     * @return contenido original si el hash es válido, null si está comprometido
     */
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
