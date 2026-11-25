package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionPerfilParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionAgregarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionIdParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionRestablecerContrasenaParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.UsuarioAdministracionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administracion/usuarios")
public class UsuarioAdministracionController {
    private final UsuarioAdministracionService service;
    private final ContextoAutenticacionService contextoService;

    public UsuarioAdministracionController(UsuarioAdministracionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody UsuarioAdministracionAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "crear");
            if (parametro == null) throw new IllegalArgumentException("Los datos del usuario son requeridos");
            return ResponseEntity.status(201).body(ApiResponse.success(201,
                    service.crear(parametro, contexto.usuarioUuid()), "Usuario creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody UsuarioAdministracionActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "modificar");
            var actual = parametro == null ? null : service.uuid(parametro.getUsuarioAdministracionUuid());
            if (actual == null) return noEncontrado();
            if (parametro.getEstado() != null && !parametro.getEstado().equals(actual.getEstado())) {
                contextoService.exigirAccionAdministracion(contexto, "usuarios", "cambiar_estado");
            }
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Usuario actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "listar");
            return ResponseEntity.ok(ApiResponse.success(200, service.all(), "Usuarios obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody UsuarioAdministracionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "listar");
            var resultado = parametro == null ? null : service.id(parametro.getUsuarioAdministracionId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Usuario obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/restablecerContrasena")
    public ResponseEntity<ApiResponse<?>> restablecerContrasena(@RequestBody UsuarioAdministracionRestablecerContrasenaParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "restablecer_contrasena");
            var usuario = parametro == null ? null : service.id(parametro.getUsuarioAdministracionId());
            if (usuario == null) return noEncontrado();
            service.restablecerContrasena(parametro.getUsuarioAdministracionId(), parametro.getContrasena(), contexto.usuarioUuid());
            return ResponseEntity.ok(ApiResponse.success(200, null, "Contraseña restablecida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getActual")
    public ResponseEntity<ApiResponse<?>> getActual(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAdministracion(contexto);
            var resultado = service.id(contexto.usuarioAdministracionId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Perfil obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/updateActual")
    public ResponseEntity<ApiResponse<?>> updateActual(@RequestBody UsuarioAdministracionPerfilParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAdministracion(contexto);
            if (parametro == null) throw new IllegalArgumentException("Los datos del perfil son requeridos");
            var resultado = service.actualizarPerfil(contexto.usuarioAdministracionId(), parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Perfil actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody UsuarioAdministracionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "usuarios", "cambiar_estado");
            var usuario = parametro == null ? null : service.id(parametro.getUsuarioAdministracionId());
            if (usuario == null) return noEncontrado();
            boolean eliminado = service.eliminar(parametro.getUsuarioAdministracionId(), contexto.usuarioUuid(), contexto.usuarioUuid());
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
