package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.ServicioPruebaParameters;
import com.aarondevs.luminis.Parameters.Administracion.ServicioSmtpParameters;
import com.aarondevs.luminis.Parameters.Administracion.ServicioWhatsappParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.ServicioNotificacionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administracion/notificaciones")
public class ServicioNotificacionController {
    private final ServicioNotificacionService service;
    private final ContextoAutenticacionService contextoService;

    public ServicioNotificacionController(ServicioNotificacionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/smtp/get")
    public ResponseEntity<ApiResponse<?>> smtpGet(HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "smtp", "ver");
            return ResponseEntity.ok(ApiResponse.success(200, service.obtener(), "Configuracion obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/smtp/save")
    public ResponseEntity<ApiResponse<?>> smtpSave(@RequestBody ServicioSmtpParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "smtp", "editar");
            return ResponseEntity.ok(ApiResponse.success(200, service.guardarSmtp(parametro, contexto.usuarioUuid()), "Servicio SMTP guardado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/smtp/probar")
    public ResponseEntity<ApiResponse<?>> smtpProbar(@RequestBody ServicioPruebaParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "smtp", "probar");
            service.probarSmtp(parametro == null ? null : parametro.getDestino());
            return ResponseEntity.ok(ApiResponse.success(200, null, "Correo de prueba enviado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/whatsapp/get")
    public ResponseEntity<ApiResponse<?>> whatsappGet(HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "whatsapp", "ver");
            return ResponseEntity.ok(ApiResponse.success(200, service.obtener(), "Configuracion obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/whatsapp/save")
    public ResponseEntity<ApiResponse<?>> whatsappSave(@RequestBody ServicioWhatsappParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "whatsapp", "editar");
            return ResponseEntity.ok(ApiResponse.success(200, service.guardarWhatsapp(parametro, contexto.usuarioUuid()), "Servicio de WhatsApp guardado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/whatsapp/probar")
    public ResponseEntity<ApiResponse<?>> whatsappProbar(@RequestBody ServicioPruebaParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "whatsapp", "probar");
            service.probarWhatsapp(parametro == null ? null : parametro.getDestino());
            return ResponseEntity.ok(ApiResponse.success(200, null, "Mensaje de prueba enviado correctamente"));
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
