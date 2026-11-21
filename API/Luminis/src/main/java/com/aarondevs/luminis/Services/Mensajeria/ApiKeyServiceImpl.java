package com.aarondevs.luminis.Services.Mensajeria;

import com.aarondevs.luminis.DAO.Mensajeria.ApiKeyDataSet;
import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyActualizarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyAgregarParameters;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {
    private static final int MAXIMO_POR_CLIENTE = 25;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ApiKeyDataSet datos;

    public ApiKeyServiceImpl(ApiKeyDataSet datos) {
        this.datos = datos;
    }

    @Override
    @Transactional
    public ApiKey crear(ApiKeyAgregarParameters parametro, Long clienteId, String actor) {
        if (parametro == null) throw new IllegalArgumentException("Los datos de la API key son requeridos");
        String nombre = nombre(parametro.getNombre());
        String permisos = permisos(parametro.getPermisos());
        if (datos.countActivas(clienteId) >= MAXIMO_POR_CLIENTE) {
            throw new IllegalArgumentException("Alcanzó el máximo de " + MAXIMO_POR_CLIENTE + " API keys activas");
        }
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        String clave = "lm_" + HexFormat.of().formatHex(bytes);
        ApiKey creada = datos.findById(datos.create(clienteId, nombre, clave.substring(0, 11), hash(clave), permisos, actor));
        creada.setClave(clave);
        return creada;
    }

    @Override
    public ApiKey actualizar(ApiKeyActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getApiKeyId() == null) throw new IllegalArgumentException("apiKeyId es requerido");
        String estado = parametro.getEstado() == null ? "Activo" : parametro.getEstado().trim();
        if (!Set.of("Activo", "Suspendido").contains(estado)) throw new IllegalArgumentException("estado debe ser Activo o Suspendido");
        datos.update(parametro.getApiKeyId(), nombre(parametro.getNombre()), permisos(parametro.getPermisos()), estado, actor);
        return datos.findById(parametro.getApiKeyId());
    }

    @Override
    public boolean revocar(Long apiKeyId, String actor) {
        return datos.revocar(apiKeyId, actor) > 0;
    }

    @Override
    public ApiKey id(Long apiKeyId) {
        return apiKeyId == null ? null : datos.findById(apiKeyId);
    }

    @Override
    public List<ApiKey> all(Long clienteId) {
        return datos.findAll(clienteId);
    }

    @Override
    public ApiKey autenticar(String clave, String permiso) {
        ApiKey apiKey = clave == null || clave.isBlank() ? null : datos.findActivaByHash(hash(clave.trim()));
        if (apiKey == null) throw new SecurityException("APIKEY NO VALIDA");
        if (!Arrays.asList(apiKey.getPermisos().split(",")).contains(permiso)) {
            throw new SecurityException("NO TIENES PERMISO PARA ESTA ACCION");
        }
        datos.registrarUso(apiKey.getApiKeyId());
        return apiKey;
    }

    private String nombre(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("El nombre de la API key es requerido");
        if (valor.trim().length() > 80) throw new IllegalArgumentException("El nombre admite hasta 80 caracteres");
        return valor.trim();
    }

    private String permisos(List<String> valores) {
        if (valores == null || valores.isEmpty()) throw new IllegalArgumentException("Seleccione al menos un permiso");
        Set<String> limpios = new LinkedHashSet<>();
        for (String valor : valores) {
            if (valor == null || !PERMISOS.contains(valor.trim())) throw new IllegalArgumentException("Permiso no válido: " + valor);
            limpios.add(valor.trim());
        }
        return String.join(",", limpios);
    }

    private String hash(String clave) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
