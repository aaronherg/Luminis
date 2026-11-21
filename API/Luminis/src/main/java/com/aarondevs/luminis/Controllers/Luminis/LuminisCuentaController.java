package com.aarondevs.luminis.Controllers.Luminis;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luminis/cuenta")
public class LuminisCuentaController {
    private final SuscripcionService suscripciones;
    private final SocketHub hub;
    private final LuminisApiSupport support;

    public LuminisCuentaController(SuscripcionService suscripciones, SocketHub hub, LuminisApiSupport support) {
        this.suscripciones = suscripciones;
        this.hub = hub;
        this.support = support;
    }

    @GetMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(HttpServletRequest request) {
        try {
            var llave = support.autenticar(request, "consultar_cuenta", false);
            Map<String, Object> consumo = suscripciones.consumo(llave.getClienteId());
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("plan", consumo.get("vigente"));
            resultado.put("peticionesUsadas", consumo.get("peticionesUsadas"));
            resultado.put("peticionesLimite", consumo.get("peticionesLimite"));
            resultado.put("peticionesIlimitadas", consumo.get("peticionesIlimitadas"));
            resultado.put("conexionesLimite", consumo.get("conexionesLimite"));
            resultado.put("conexionesIlimitadas", consumo.get("conexionesIlimitadas"));
            resultado.put("enVivo", hub.resumen(llave.getClienteId()));
            resultado.put("permisos", llave.getPermisos().split(","));
            return ResponseEntity.ok(ApiResponse.success(200, resultado, "Cuenta obtenida correctamente"));
        } catch (Exception error) {
            return support.error(error);
        }
    }
}
