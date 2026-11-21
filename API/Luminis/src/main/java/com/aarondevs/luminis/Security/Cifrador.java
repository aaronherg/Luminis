package com.aarondevs.luminis.Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cifrador {
    private static final int IV_LENGTH = 12;
    private final SecretKey clave;

    public Cifrador(SecretKey clave) {
        if (clave == null || clave.getEncoded() == null || clave.getEncoded().length == 0) {
            throw new IllegalStateException("La clave de cifrado no contiene material utilizable");
        }
        byte[] material = clave.getEncoded();
        if (material.length != 16 && material.length != 24 && material.length != 32) {
            try {
                material = MessageDigest.getInstance("SHA-256").digest(material);
            } catch (Exception error) {
                throw new IllegalStateException("No fue posible preparar la clave de cifrado", error);
            }
        }
        this.clave = new SecretKeySpec(material, "AES");
    }

    public String cifrar(String texto) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        byte[] combinado = new byte[iv.length + cifrado.length];
        System.arraycopy(iv, 0, combinado, 0, iv.length);
        System.arraycopy(cifrado, 0, combinado, iv.length, cifrado.length);
        return Base64.getEncoder().encodeToString(combinado);
    }

    public String descifrar(String valor) throws Exception {
        byte[] combinado = Base64.getDecoder().decode(valor);
        if (combinado.length <= IV_LENGTH) throw new IllegalArgumentException("Texto cifrado inválido");
        byte[] iv = java.util.Arrays.copyOfRange(combinado, 0, IV_LENGTH);
        byte[] cifrado = java.util.Arrays.copyOfRange(combinado, IV_LENGTH, combinado.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }
}
