package com.aarondevs.luminis.Componentes.Seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretoCifrador {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final SecretKeySpec clave;

    public SecretoCifrador(@Value("${NOTIFICACION_SECRET:}") String notificacionSecret,
                           @Value("${JWT_SECRET:}") String jwtSecret) throws Exception {
        String base = !vacio(notificacionSecret) ? notificacionSecret
                : (!vacio(jwtSecret) ? jwtSecret : "luminis-notificacion-clave-por-defecto");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        this.clave = new SecretKeySpec(sha.digest(base.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    public String cifrar(String texto) throws Exception {
        if (texto == null) return null;
        byte[] iv = new byte[12];
        ALEATORIO.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        byte[] salida = new byte[iv.length + cifrado.length];
        System.arraycopy(iv, 0, salida, 0, iv.length);
        System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
        return Base64.getEncoder().encodeToString(salida);
    }

    public String descifrar(String valor) throws Exception {
        if (valor == null || valor.isBlank()) return null;
        byte[] datos = Base64.getDecoder().decode(valor);
        byte[] iv = new byte[12];
        System.arraycopy(datos, 0, iv, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(128, iv));
        byte[] claro = cipher.doFinal(datos, 12, datos.length - 12);
        return new String(claro, StandardCharsets.UTF_8);
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}
