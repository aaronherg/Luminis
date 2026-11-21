package com.aarondevs.luminis.Controllers.Clientes;

import com.aarondevs.luminis.Parameters.Clientes.ClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Clientes.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes/registro")
public class RegistroController {
    private final ClienteService service;

    public RegistroController(ClienteService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody ClienteAgregarParameters parametro) {
        try {
            if (parametro != null) parametro.setDiaCorte(null);
            var cliente = service.crear(parametro, "REGISTRO");
            return ResponseEntity.status(201).body(ApiResponse.success(201, cliente.getClienteUuid(),
                    "Cuenta creada correctamente. Ya puede iniciar sesión"));
        } catch (Exception error) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
        }
    }
}
