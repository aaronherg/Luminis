package com.aarondevs.luminis.Controllers.Seguridad;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionCambiarContrasenaParameters;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionLoginParameters;
import com.aarondevs.luminis.Services.Seguridad.AutenticacionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguridad/autenticacion")
public class AutenticacionController {
    private final AutenticacionService service;
    private final ContextoAutenticacionService contextoService;

    public AutenticacionController(AutenticacionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody AutenticacionLoginParameters parametro, HttpServletRequest request) {
        if (parametro == null || parametro.getUsuario() == null || parametro.getUsuario().isBlank()
                || parametro.getContrasena() == null || parametro.getContrasena().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "usuario y contrasena son requeridos"));
        }
        try {
            parametro.setIpPublica(ip(request));
            parametro.setAgente(request.getHeader(HttpHeaders.USER_AGENT));
            var resultado = service.autenticar(parametro);
            return resultado == null
                    ? ResponseEntity.status(401).body(ApiResponse.error(401, "Usuario o contraseña incorrectos"))
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Autenticación realizada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getSesion")
    public ResponseEntity<ApiResponse<?>> getSesion(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(200, service.sesionActual(contextoService.resolver(request)),
                    "Sesión obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request) {
        try {
            service.cerrar(contextoService.resolver(request));
            return ResponseEntity.ok(ApiResponse.success(200, null, "Sesión cerrada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/cambiarContrasena")
    public ResponseEntity<ApiResponse<?>> cambiarContrasena(@RequestBody AutenticacionCambiarContrasenaParameters parametro, HttpServletRequest request) {
        try {
            service.cambiarContrasena(contextoService.resolver(request), parametro);
            return ResponseEntity.ok(ApiResponse.success(200, null, "Contraseña actualizada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    private String ip(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        return reenviada != null && !reenviada.isBlank() ? reenviada.split(",")[0].trim() : request.getRemoteAddr();
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
