package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.DAO.Seguridad.AutenticacionDataSet;
import com.aarondevs.luminis.Models.Seguridad.Usuario;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionCambiarContrasenaParameters;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionLoginParameters;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import com.aarondevs.luminis.Security.JwtService;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {
    private final AutenticacionDataSet datos;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ContrasenaService contrasenaService;
    private final ContextoAutenticacionService contextoService;

    public AutenticacionServiceImpl(AutenticacionDataSet datos, JwtService jwtService, PasswordEncoder passwordEncoder,
                                ContrasenaService contrasenaService, ContextoAutenticacionService contextoService) {
        this.datos = datos;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaService = contrasenaService;
        this.contextoService = contextoService;
    }

    @Override
    public Map<String, Object> autenticar(AutenticacionLoginParameters parametro) {
        Usuario usuario = datos.findUsuarioByUsuario(parametro.getUsuario().trim());
        if (usuario == null || !passwordEncoder.matches(parametro.getContrasena(), usuario.getContrasena())) return null;
        if (!"Activo".equalsIgnoreCase(usuario.getEstado())) {
            throw new SecurityException("El usuario se encuentra suspendido");
        }
        Map<String, Object> perfil = perfil(usuario);
        if (perfil == null || !"Activo".equalsIgnoreCase(String.valueOf(perfil.get("estado")))) {
            throw new SecurityException("El usuario se encuentra suspendido");
        }
        if (perfil.containsKey("clienteEstado") && !"Activo".equalsIgnoreCase(String.valueOf(perfil.get("clienteEstado")))) {
            throw new SecurityException("La cuenta del cliente se encuentra suspendida");
        }

        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("usuarioUuid", usuario.getUsuarioUuid());
        claims.put("tipo", usuario.getTipo());
        String token = jwtService.generarJwt(usuario.getUsuarioUuid(), claims, jti);
        Timestamp expiracion = new Timestamp(jwtService.obtenerClaims(token).getExpiration().getTime());
        datos.crear(usuario.getUsuarioId(), usuario.getUsuarioUuid(), usuario.getTipo(), jti,
                parametro.getIpPublica(), parametro.getAgente(), expiracion);
        datos.registrarAcceso(usuario.getUsuarioId());

        ContextoAutenticacion contexto = contextoParaSesion(usuario, perfil, jti);
        Map<String, Object> resultado = sesion(usuario, perfil, contextoService.acciones(contexto));
        resultado.put("token", token);
        resultado.put("fechaExpiracion", expiracion);
        return resultado;
    }

    @Override
    public Map<String, Object> sesionActual(ContextoAutenticacion contexto) {
        Usuario usuario = datos.findUsuarioById(contexto.usuarioId());
        return sesion(usuario, perfil(usuario), contextoService.acciones(contexto));
    }

    @Override
    public boolean cerrar(ContextoAutenticacion contexto) {
        return datos.cerrar(contexto.tokenJti(), "Cierre de sesión del usuario");
    }

    @Override
    public void cambiarContrasena(ContextoAutenticacion contexto, AutenticacionCambiarContrasenaParameters parametro) {
        if (parametro == null || parametro.getContrasenaActual() == null || parametro.getContrasenaNueva() == null) {
            throw new IllegalArgumentException("contrasenaActual y contrasenaNueva son requeridas");
        }
        Usuario usuario = datos.findUsuarioById(contexto.usuarioId());
        if (usuario == null || !passwordEncoder.matches(parametro.getContrasenaActual(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        contrasenaService.validar(parametro.getContrasenaNueva());
        datos.actualizarContrasena(usuario.getUsuarioId(), passwordEncoder.encode(parametro.getContrasenaNueva()), usuario.getUsuarioUuid());
    }

    private Map<String, Object> perfil(Usuario usuario) {
        if (usuario == null) return null;
        return ContextoAutenticacion.ADMINISTRACION.equalsIgnoreCase(usuario.getTipo())
                ? datos.perfilAdministracion(usuario.getUsuarioId())
                : datos.perfilCliente(usuario.getUsuarioId());
    }

    private ContextoAutenticacion contextoParaSesion(Usuario usuario, Map<String, Object> perfil, String jti) {
        if (ContextoAutenticacion.ADMINISTRACION.equalsIgnoreCase(usuario.getTipo())) {
            return new ContextoAutenticacion(usuario.getUsuarioId(), usuario.getUsuarioUuid(), usuario.getTipo(), jti,
                    null, null, null, null, numero(perfil.get("usuarioAdministracionId")), texto(perfil.get("usuarioAdministracionUuid")));
        }
        return new ContextoAutenticacion(usuario.getUsuarioId(), usuario.getUsuarioUuid(), usuario.getTipo(), jti,
                numero(perfil.get("clienteId")), texto(perfil.get("clienteUuid")),
                numero(perfil.get("usuarioClienteId")), texto(perfil.get("usuarioClienteUuid")), null, null);
    }

    private Map<String, Object> sesion(Usuario usuario, Map<String, Object> perfil, Set<String> acciones) {
        boolean administracion = ContextoAutenticacion.ADMINISTRACION.equalsIgnoreCase(usuario.getTipo());
        List<Map<String, Object>> modulos = datos.modulos(administracion ? "Administracion" : "Clientes").stream()
                .filter(m -> acciones.stream().anyMatch(a -> a.startsWith(m.get("clave") + ".")))
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("usuarioUuid", usuario.getUsuarioUuid());
        resultado.put("usuario", usuario.getUsuario());
        resultado.put("tipo", usuario.getTipo());
        resultado.put("destino", administracion ? "/pages/center/" : "/pages/customer/");
        resultado.put("debeCambiarContrasena", Boolean.TRUE.equals(usuario.getDebeCambiarContrasena()));
        resultado.put("perfil", perfil);
        resultado.put("acciones", acciones);
        resultado.put("modulos", modulos);
        return resultado;
    }

    private Long numero(Object valor) {
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
