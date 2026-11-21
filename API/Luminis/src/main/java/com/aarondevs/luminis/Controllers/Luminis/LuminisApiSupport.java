package com.aarondevs.luminis.Controllers.Luminis;

import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.ApiKeyService;
import com.aarondevs.luminis.Services.Mensajeria.ConsumoMensajeria;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class LuminisApiSupport {
    public static final String ENCABEZADO = "X-Api-Key";

    private final ApiKeyService apiKeys;
    private final ConsumoMensajeria consumo;

    public LuminisApiSupport(ApiKeyService apiKeys, ConsumoMensajeria consumo) {
        this.apiKeys = apiKeys;
        this.consumo = consumo;
    }

    public ApiKey autenticar(HttpServletRequest request, String permiso, boolean contarPeticion) {
        String clave = request.getHeader(ENCABEZADO);
        if (clave == null || clave.isBlank()) throw new ApiKeyRequeridaException("Envíe su API key en el encabezado " + ENCABEZADO);
        ApiKey llave;
        try {
            llave = apiKeys.autenticar(clave.trim(), permiso);
        } catch (SecurityException error) {
            if ("APIKEY NO VALIDA".equals(error.getMessage())) throw new ApiKeyRequeridaException("La API key no es válida o está suspendida");
            throw new SecurityException("La API key no tiene el permiso " + permiso);
        }
        consumo.sumar(llave.getClienteId(), "solicitudesApi", 1);
        if (!consumo.limites(llave.getClienteId()).vigente()) throw new SinPlanException("La cuenta no tiene un plan vigente");
        if (contarPeticion && !consumo.consumirPeticion(llave.getClienteId())) {
            consumo.sumar(llave.getClienteId(), "rechazados", 1);
            throw new SocketHub.LimiteException("Se alcanzó el límite de peticiones del plan");
        }
        return llave;
    }

    public ResponseEntity<ApiResponse<?>> error(Exception error) {
        int estado = error instanceof ApiKeyRequeridaException ? 401
                : error instanceof SinPlanException ? 402
                : error instanceof SecurityException ? 403
                : error instanceof SocketHub.LimiteException ? 429
                : error instanceof IllegalStateException ? 500 : 400;
        return ResponseEntity.status(estado).body(ApiResponse.error(estado, error.getMessage() == null ? "Solicitud no válida" : error.getMessage()));
    }

    public static class ApiKeyRequeridaException extends RuntimeException {
        public ApiKeyRequeridaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SinPlanException extends RuntimeException {
        public SinPlanException(String mensaje) {
            super(mensaje);
        }
    }
}
