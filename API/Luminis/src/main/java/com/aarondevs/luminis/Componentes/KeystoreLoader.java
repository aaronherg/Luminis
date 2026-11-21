package com.aarondevs.luminis.Componentes;

import java.io.InputStream;
import java.security.KeyStore;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class KeystoreLoader {
    private final ResourceLoader resourceLoader;

    @Value("${app.keystore.path}")
    private String rutaKeystore;
    @Value("${app.keystore.password}")
    private String passwordKeystore;
    @Value("${app.keystore.alias}")
    private String aliasClave;
    @Value("${app.keystore.key-password}")
    private String passwordClave;

    public KeystoreLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public SecretKey cargarClave() throws Exception {
        if (rutaKeystore == null || rutaKeystore.isBlank()
                || passwordKeystore == null || passwordKeystore.isBlank()
                || aliasClave == null || aliasClave.isBlank()
                || passwordClave == null || passwordClave.isBlank()) {
            throw new IllegalStateException("Configuración de keystore incompleta");
        }
        KeyStore keystore = KeyStore.getInstance("JCEKS");
        Resource recurso = resourceLoader.getResource(rutaKeystore);
        if (!recurso.exists()) {
            throw new IllegalStateException("No se encontró el keystore configurado");
        }
        try (InputStream entrada = recurso.getInputStream()) {
            keystore.load(entrada, passwordKeystore.toCharArray());
        }
        var clave = keystore.getKey(aliasClave, passwordClave.toCharArray());
        if (!(clave instanceof SecretKey secretKey)) {
            throw new IllegalStateException("El alias no contiene una SecretKey válida");
        }
        return secretKey;
    }
}
