package com.aarondevs.luminis.Controllers.Luminis;

import com.aarondevs.luminis.Parameters.Mensajeria.PresenciaBuscarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.PresenciaIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luminis/presencia")
public class LuminisPresenciaController {
    private final SocketHub hub;
    private final LuminisApiSupport support;

    public LuminisPresenciaController(SocketHub hub, LuminisApiSupport support) {
        this.hub = hub;
        this.support = support;
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestBody(required = false) PresenciaBuscarParameters parametro, HttpServletRequest request) {
        try {
            var llave = support.autenticar(request, "consultar_presencia", true);
            Map<String, Object> resultado = new LinkedHashMap<>();
            if (parametro != null && parametro.getUsuarios() != null && !parametro.getUsuarios().isEmpty()) {
                if (parametro.getUsuarios().size() > 500) throw new IllegalArgumentException("Consulte hasta 500 usuarios por petición");
                Map<String, Object> estados = new LinkedHashMap<>();
                parametro.getUsuarios().forEach(u -> estados.put(u, hub.enLinea(llave.getClienteId(), u)));
                resultado.put("usuarios", estados);
            } else {
                int limite = parametro == null || parametro.getLimite() == null ? 500 : Math.max(1, Math.min(parametro.getLimite(), 5000));
                resultado.put("enLinea", hub.usuariosEnLinea(llave.getClienteId(), limite));
            }
            resultado.putAll(hub.resumen(llave.getClienteId()));
            return ResponseEntity.ok(ApiResponse.success(200, resultado, "Presencia obtenida correctamente"));
        } catch (Exception error) {
            return support.error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody(required = false) PresenciaIdParameters parametro, HttpServletRequest request) {
        try {
            var llave = support.autenticar(request, "consultar_presencia", true);
            if (parametro == null || parametro.getUsuario() == null || parametro.getUsuario().isBlank()) throw new IllegalArgumentException("usuario es requerido");
            String usuario = parametro.getUsuario().trim();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("usuario", usuario);
            resultado.put("enLinea", hub.enLinea(llave.getClienteId(), usuario));
            resultado.put("conexiones", hub.conexionesDe(llave.getClienteId(), usuario));
            return ResponseEntity.ok(ApiResponse.success(200, resultado, "Presencia obtenida correctamente"));
        } catch (Exception error) {
            return support.error(error);
        }
    }
}
