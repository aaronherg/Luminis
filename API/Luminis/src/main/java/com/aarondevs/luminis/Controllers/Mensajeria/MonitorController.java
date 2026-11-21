package com.aarondevs.luminis.Controllers.Mensajeria;

import com.aarondevs.luminis.Parameters.Mensajeria.MonitorBuscarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.MonitorDesconectarParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mensajeria/monitor")
public class MonitorController {
    private final SocketHub hub;
    private final ContextoAutenticacionService contextoService;

    public MonitorController(SocketHub hub, ContextoAutenticacionService contextoService) {
        this.hub = hub;
        this.contextoService = contextoService;
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestBody(required = false) MonitorBuscarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "monitor", "consultar", "monitor", "consultar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro == null ? null : parametro.getClienteId());
            Map<String, Object> resultado = new LinkedHashMap<>(hub.resumen(clienteId));
            resultado.put("sesiones", hub.sesiones(clienteId));
            return ResponseEntity.ok(ApiResponse.success(200, resultado, "Conexiones obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody(required = false) MonitorDesconectarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "monitor", "desconectar", "monitor", "desconectar");
            if (parametro == null) throw new IllegalArgumentException("Indique la sesión o el usuario a desconectar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro.getClienteId());
            String motivo = contexto.esAdministracion() ? "Desconectado por la administración de Luminis" : "Desconectado desde el panel";
            int cerradas;
            if (parametro.getSesion() != null && !parametro.getSesion().isBlank()) {
                cerradas = hub.cerrarSesion(clienteId, parametro.getSesion().trim(), motivo) ? 1 : 0;
            } else {
                if (clienteId == null || parametro.getUsuario() == null || parametro.getUsuario().isBlank()) {
                    throw new IllegalArgumentException("clienteId y usuario son requeridos");
                }
                cerradas = hub.desconectar(clienteId, parametro.getUsuario().trim(), motivo);
            }
            if (cerradas == 0) throw new IllegalArgumentException("No hay conexiones activas para desconectar");
            return ResponseEntity.ok(ApiResponse.success(200, Map.of("conexionesCerradas", cerradas), "Conexión cerrada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
