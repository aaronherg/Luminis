package com.aarondevs.luminis.Controllers.Clientes;

import com.aarondevs.luminis.Parameters.Clientes.ClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteBuscarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Clientes.ClienteService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService service;
    private final ContextoAutenticacionService contextoService;

    public ClienteController(ClienteService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody ClienteAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "clientes", "crear");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.usuarioUuid()),
                    "Cliente creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody ClienteActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "clientes", "modificar");
            var actual = parametro == null ? null : service.uuid(parametro.getClienteUuid());
            if (actual == null) return noEncontrado();
            if (parametro.getEstado() != null && !parametro.getEstado().equals(actual.getEstado())) {
                contextoService.exigirAccionAdministracion(contexto, "clientes", "cambiar_estado");
            }
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Cliente actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAllPages")
    public ResponseEntity<ApiResponse<?>> getAllPages(@RequestBody ClienteBuscarParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "clientes", "listar");
            return ResponseEntity.ok(ApiResponse.success(200,
                    service.pages(parametro == null ? null : parametro.getPage(), parametro == null ? null : parametro.getSize(),
                            parametro == null ? null : parametro.getBuscar(), parametro == null ? null : parametro.getEstado()),
                    "Clientes obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody ClienteIdParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "clientes", "listar");
            Object resultado = parametro == null ? null : service.id(parametro.getClienteId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Cliente obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getActual")
    public ResponseEntity<ApiResponse<?>> getActual(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirCliente(contexto);
            return ResponseEntity.ok(ApiResponse.success(200, service.id(contexto.clienteId()), "Cliente obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/updateActual")
    public ResponseEntity<ApiResponse<?>> updateActual(@RequestBody ClienteActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "perfil", "modificar");
            if (parametro == null) throw new IllegalArgumentException("Los datos del cliente son requeridos");
            var actual = service.id(contexto.clienteId());
            parametro.setClienteUuid(actual.getClienteUuid());
            parametro.setEstado(null);
            parametro.setDiaCorte(null);
            return ResponseEntity.ok(ApiResponse.success(200, service.actualizar(parametro, contexto.usuarioUuid()),
                    "Perfil actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody ClienteIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "clientes", "cambiar_estado");
            boolean eliminado = parametro != null && service.eliminar(parametro.getClienteId(), contexto.usuarioUuid());
            return eliminado ? ResponseEntity.ok(ApiResponse.success(200, null, "Cliente eliminado correctamente")) : noEncontrado();
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Cliente no encontrado"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
