package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.DAO.Seguridad.ContextoAutenticacionDataSet;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import com.aarondevs.luminis.Security.JwtValidationFilter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ContextoAutenticacionService {
    private static final String MENSAJE_RESTRINGIDO = "Usted no tiene permiso para ejecutar esta acción";
    private static final String ATRIBUTO_CONTEXTO = "luminis.contexto";

    private final ContextoAutenticacionDataSet datos;
    private final com.aarondevs.luminis.Services.Administracion.ApiKeyAdministracionService llavesAdmin;

    public ContextoAutenticacionService(ContextoAutenticacionDataSet datos, com.aarondevs.luminis.Services.Administracion.ApiKeyAdministracionService llavesAdmin) {
        this.datos = datos;
        this.llavesAdmin = llavesAdmin;
    }

    public ContextoAutenticacion resolver(HttpServletRequest request) {
        Object guardado = request == null ? null : request.getAttribute(ATRIBUTO_CONTEXTO);
        if (guardado instanceof ContextoAutenticacion contexto) return contexto;
        Object llaveAdmin = request == null ? null : request.getAttribute(JwtValidationFilter.LLAVE_ADMIN_ATTRIBUTE);
        if (llaveAdmin instanceof String claveAdmin) {
            ContextoAutenticacion porLlave = llavesAdmin.autenticar(claveAdmin);
            if (porLlave == null) throw new SecurityException("La API key de administración no es válida o está revocada");
            request.setAttribute(ATRIBUTO_CONTEXTO, porLlave);
            return porLlave;
        }
        Object valor = request == null ? null : request.getAttribute(JwtValidationFilter.CLAIMS_ATTRIBUTE);
        if (!(valor instanceof Claims claims)) {
            throw new SecurityException("No se pudo determinar el contexto autenticado");
        }
        ContextoAutenticacion contexto = datos.resolver(
                texto(claims.get("usuarioUuid")), texto(claims.get("tipo")), claims.getId());
        if (contexto == null) {
            throw new SecurityException("La sesión no está activa. Inicie sesión nuevamente");
        }
        datos.registrarActividad(contexto.tokenJti());
        request.setAttribute(ATRIBUTO_CONTEXTO, contexto);
        return contexto;
    }

    public Set<String> acciones(ContextoAutenticacion contexto) {
        if (contexto.esAdministracion()) return datos.accionesAdministracion(contexto.usuarioAdministracionId());
        if (contexto.esCliente()) return datos.accionesCliente(contexto.usuarioClienteId());
        return Set.of();
    }

    public void exigirAdministracion(ContextoAutenticacion contexto) {
        if (contexto == null || !contexto.esAdministracion()) {
            throw new SecurityException("Esta operación requiere un usuario de Administración");
        }
    }

    public void exigirCliente(ContextoAutenticacion contexto) {
        if (contexto == null || !contexto.esCliente()) {
            throw new SecurityException("Esta operación requiere un usuario de cliente");
        }
    }

    public void exigirAccionAdministracion(ContextoAutenticacion contexto, String modulo, String accion) {
        exigirAdministracion(contexto);
        if (!datos.accionesAdministracion(contexto.usuarioAdministracionId()).contains(modulo + "." + accion)) {
            throw new SecurityException(MENSAJE_RESTRINGIDO);
        }
    }

    public void exigirAccionCliente(ContextoAutenticacion contexto, String modulo, String accion) {
        exigirCliente(contexto);
        if (!datos.accionesCliente(contexto.usuarioClienteId()).contains(modulo + "." + accion)) {
            throw new SecurityException(MENSAJE_RESTRINGIDO);
        }
    }

    public void exigirAccion(ContextoAutenticacion contexto, String moduloAdministracion, String accionAdministracion,
                             String moduloCliente, String accionCliente) {
        if (contexto != null && contexto.esAdministracion()) {
            exigirAccionAdministracion(contexto, moduloAdministracion, accionAdministracion);
        } else {
            exigirAccionCliente(contexto, moduloCliente, accionCliente);
        }
    }

    public Long clienteDeContexto(ContextoAutenticacion contexto, Long clienteIdSolicitado) {
        if (contexto.esCliente()) {
            if (clienteIdSolicitado != null && !clienteIdSolicitado.equals(contexto.clienteId())) {
                throw new SecurityException("El recurso solicitado no pertenece al cliente autenticado");
            }
            return contexto.clienteId();
        }
        return clienteIdSolicitado;
    }

    public void validarCliente(ContextoAutenticacion contexto, Long clienteIdRecurso) {
        if (contexto.esCliente() && (clienteIdRecurso == null || !clienteIdRecurso.equals(contexto.clienteId()))) {
            throw new SecurityException("El recurso solicitado no pertenece al cliente autenticado");
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
