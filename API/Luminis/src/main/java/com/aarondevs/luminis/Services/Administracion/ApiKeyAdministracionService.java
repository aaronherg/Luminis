package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.DAO.Administracion.ApiKeyAdministracionDataSet;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyAdministracionService {
    public static final String ENCABEZADO = "X-Admin-Key";
    private static final String PREFIJO = "lumadm_";
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final ApiKeyAdministracionDataSet datos;

    public ApiKeyAdministracionService(ApiKeyAdministracionDataSet datos) {
        this.datos = datos;
    }

    public List<Map<String, Object>> listar(ContextoAutenticacion contexto) {
        exigir(contexto);
        return datos.listar(contexto.usuarioAdministracionId());
    }

    public Map<String, Object> crear(ContextoAutenticacion contexto, String nombre) {
        exigir(contexto);
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty() || limpio.length() > 120) throw new IllegalArgumentException("Escribe un nombre para la API key (máximo 120 caracteres)");
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        String clave = PREFIJO + HexFormat.of().formatHex(bytes);
        String uuid = UUID.randomUUID().toString();
        datos.crear(uuid, contexto.usuarioAdministracionId(), limpio, clave.substring(0, 14), hash(clave), contexto.usuarioUuid());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("uuid", uuid);
        r.put("nombre", limpio);
        r.put("prefijo", clave.substring(0, 14));
        r.put("clave", clave);
        r.put("encabezado", ENCABEZADO);
        return r;
    }

    public Map<String, Object> renombrar(ContextoAutenticacion contexto, String uuid, String nombre) {
        exigir(contexto);
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty() || limpio.length() > 120) throw new IllegalArgumentException("Escribe un nombre para la API key (máximo 120 caracteres)");
        if (datos.renombrar(uuid, contexto.usuarioAdministracionId(), limpio, contexto.usuarioUuid()) == 0) throw new IllegalArgumentException("La API key no existe");
        return datos.porUuid(uuid, contexto.usuarioAdministracionId());
    }

    public void revocar(ContextoAutenticacion contexto, String uuid) {
        exigir(contexto);
        if (datos.cambiarEstado(uuid, contexto.usuarioAdministracionId(), "Revocada", contexto.usuarioUuid()) == 0) throw new IllegalArgumentException("La API key no existe");
    }

    public ContextoAutenticacion autenticar(String clave) {
        if (clave == null || !clave.trim().startsWith(PREFIJO)) return null;
        String hash = hash(clave.trim());
        ContextoAutenticacion contexto = datos.resolver(hash);
        if (contexto != null) datos.registrarUso(hash);
        return contexto;
    }

    private void exigir(ContextoAutenticacion contexto) {
        if (contexto == null || !contexto.esAdministracion() || contexto.usuarioAdministracionId() == null) {
            throw new SecurityException("Esta operación requiere un usuario de Administración");
        }
    }

    private String hash(String clave) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
