package com.aarondevs.luminis.Controllers.Clientes;

import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteIdParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePerfilParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePorClienteParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteRestablecerContrasenaParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Clientes.UsuarioClienteService;
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
@RequestMapping("/api/clientes/usuarios")
public class UsuarioClienteController {
    private final UsuarioClienteService service;
    private final ContextoAutenticacionService contextoService;

    public UsuarioClienteController(UsuarioClienteService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody UsuarioClienteAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "usuarios", "crear");
            if (parametro == null) throw new IllegalArgumentException("Los datos del usuario son requeridos");
            parametro.setClienteId(contextoService.clienteDeContexto(contexto, parametro.getClienteId()));
            if (parametro.getClienteId() == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.status(201).body(ApiResponse.success(201,
                    service.crear(parametro, false, contexto.usuarioUuid()), "Usuario creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody UsuarioClienteActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "usuarios", "modificar");
            var actual = parametro == null ? null : service.uuid(parametro.getUsuarioClienteUuid());
            if (actual == null) return noEncontrado();
            contextoService.validarCliente(contexto, actual.getClienteId());
            parametro.setClienteId(actual.getClienteId());
            if (parametro.getEstado() != null && !parametro.getEstado().equals(actual.getEstado())) {
                contextoService.exigirAccion(contexto, "clientes", "cambiar_estado", "usuarios", "cambiar_estado");
            }
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Usuario actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestBody UsuarioClientePorClienteParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "listar", "usuarios", "listar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro == null ? null : parametro.getClienteId());
            if (clienteId == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.ok(ApiResponse.success(200, service.all(clienteId), "Usuarios obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody UsuarioClienteIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "listar", "usuarios", "listar");
            var resultado = parametro == null ? null : service.id(parametro.getUsuarioClienteId());
            if (resultado != null) contextoService.validarCliente(contexto, resultado.getClienteId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Usuario obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/restablecerContrasena")
    public ResponseEntity<ApiResponse<?>> restablecerContrasena(@RequestBody UsuarioClienteRestablecerContrasenaParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "usuarios", "modificar");
            var usuario = parametro == null ? null : service.id(parametro.getUsuarioClienteId());
            if (usuario == null) return noEncontrado();
            contextoService.validarCliente(contexto, usuario.getClienteId());
            service.restablecerContrasena(parametro.getUsuarioClienteId(), parametro.getContrasena(), contexto.usuarioUuid());
            return ResponseEntity.ok(ApiResponse.success(200, null, "Contraseña restablecida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getActual")
    public ResponseEntity<ApiResponse<?>> getActual(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirCliente(contexto);
            var resultado = service.id(contexto.usuarioClienteId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Perfil obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/updateActual")
    public ResponseEntity<ApiResponse<?>> updateActual(@RequestBody UsuarioClientePerfilParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirCliente(contexto);
            if (parametro == null) throw new IllegalArgumentException("Los datos del perfil son requeridos");
            var resultado = service.actualizarPerfil(contexto.usuarioClienteId(), parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Perfil actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody UsuarioClienteIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "cambiar_estado", "usuarios", "cambiar_estado");
            var usuario = parametro == null ? null : service.id(parametro.getUsuarioClienteId());
            if (usuario == null) return noEncontrado();
            contextoService.validarCliente(contexto, usuario.getClienteId());
            boolean eliminado = service.eliminar(parametro.getUsuarioClienteId(), contexto.usuarioUuid(), contexto.usuarioUuid());
            return eliminado ? ResponseEntity.ok(ApiResponse.success(200, null, "Usuario eliminado correctamente")) : noEncontrado();
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Usuario no encontrado"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
