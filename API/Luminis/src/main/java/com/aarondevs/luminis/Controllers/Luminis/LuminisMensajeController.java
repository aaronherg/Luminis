package com.aarondevs.luminis.Controllers.Luminis;

import com.aarondevs.luminis.Parameters.Mensajeria.MensajeAgregarParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luminis/mensajes")
public class LuminisMensajeController {
    private final SocketHub hub;
    private final LuminisApiSupport support;
    private final ObjectMapper json = new ObjectMapper();

    public LuminisMensajeController(SocketHub hub, LuminisApiSupport support) {
        this.hub = hub;
        this.support = support;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody(required = false) MensajeAgregarParameters parametro, HttpServletRequest request) {
        try {
            var llave = support.autenticar(request, "publicar", false);
            if (parametro == null) throw new IllegalArgumentException("origen, destino y mensaje son requeridos");
            int bytes = json.writeValueAsString(parametro.getMensaje()).getBytes(StandardCharsets.UTF_8).length;
            return ResponseEntity.status(201).body(ApiResponse.success(201, hub.publicar(llave.getClienteId(), parametro.getOrigen(),
                    parametro.getDestino(), parametro.getMensaje(), bytes), "Mensaje publicado"));
        } catch (Exception error) {
            return support.error(error);
        }
    }
}
