package com.aarondevs.luminis.Controllers.Luminis;

import com.aarondevs.luminis.Parameters.Mensajeria.PresenciaIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luminis/conexiones")
public class LuminisConexionController {
    private final SocketHub hub;
    private final LuminisApiSupport support;

    public LuminisConexionController(SocketHub hub, LuminisApiSupport support) {
        this.hub = hub;
        this.support = support;
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody(required = false) PresenciaIdParameters parametro, HttpServletRequest request) {
        try {
            var llave = support.autenticar(request, "desconectar", true);
            if (parametro == null || parametro.getUsuario() == null || parametro.getUsuario().isBlank()) throw new IllegalArgumentException("usuario es requerido");
            int cerradas = hub.desconectar(llave.getClienteId(), parametro.getUsuario().trim(), "Desconectado por la aplicación");
            return ResponseEntity.ok(ApiResponse.success(200, Map.of("conexionesCerradas", cerradas), "Usuario desconectado"));
        } catch (Exception error) {
            return support.error(error);
        }
    }
}
