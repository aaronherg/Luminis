package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.Componentes.Correo.CorreoService;
import com.aarondevs.luminis.DAO.Seguridad.AutenticacionDataSet;
import com.aarondevs.luminis.DAO.Seguridad.CodigoVerificacionDataSet;
import com.aarondevs.luminis.Models.Seguridad.Usuario;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionCambiarContrasenaParameters;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionDosFactoresParameters;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionLoginParameters;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import com.aarondevs.luminis.Security.JwtService;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {
    private static final String INGRESO_2FA = "IngresoDosFactores";
    private static final String ACTIVACION_2FA = "ActivacionDosFactores";
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final AutenticacionDataSet datos;
    private final CodigoVerificacionDataSet codigos;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ContrasenaService contrasenaService;
    private final ContextoAutenticacionService contextoService;
    private final CorreoService correo;

    public AutenticacionServiceImpl(AutenticacionDataSet datos, CodigoVerificacionDataSet codigos, JwtService jwtService,
                                    PasswordEncoder passwordEncoder, ContrasenaService contrasenaService,
                                    ContextoAutenticacionService contextoService, CorreoService correo) {
        this.datos = datos;
        this.codigos = codigos;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaService = contrasenaService;
        this.contextoService = contextoService;
        this.correo = correo;
    }

    @Override
    public Map<String, Object> autenticar(AutenticacionLoginParameters parametro) {
        Usuario usuario = datos.findUsuarioByUsuario(parametro.getUsuario().trim());
        if (usuario == null || !passwordEncoder.matches(parametro.getContrasena(), usuario.getContrasena())) return null;
        Map<String, Object> perfil = validarEstado(usuario);
        if (Boolean.TRUE.equals(usuario.getDosFactores())) {
            String destino = texto(perfil.get("correo"));
            if (!vacio(destino) && correo.configurado()) {
                enviarCodigoIngreso(usuario, destino);
                Map<String, Object> reto = new LinkedHashMap<>();
                reto.put("requiereDosFactores", true);
                reto.put("correo", ocultarCorreo(destino));
                return reto;
            }
        }
        return emitirSesion(usuario, perfil, parametro);
    }

    @Override
    public Map<String, Object> autenticarDosFactores(AutenticacionLoginParameters parametro) {
        if (parametro == null || vacio(parametro.getUsuario()) || vacio(parametro.getContrasena()) || vacio(parametro.getCodigo())) {
            throw new IllegalArgumentException("usuario, contrasena y codigo son requeridos");
        }
        Usuario usuario = datos.findUsuarioByUsuario(parametro.getUsuario().trim());
        if (usuario == null || !passwordEncoder.matches(parametro.getContrasena(), usuario.getContrasena())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }
        Map<String, Object> perfil = validarEstado(usuario);
        Map<String, Object> codigo = codigos.vigente(usuario.getUsuarioId(), INGRESO_2FA);
        if (codigo == null) throw new IllegalArgumentException("El código expiró o no es válido. Vuelve a iniciar sesión");
        Long codigoId = ((Number) codigo.get("codigoVerificacionId")).longValue();
        if (!passwordEncoder.matches(parametro.getCodigo().trim().toUpperCase(), String.valueOf(codigo.get("codigoHash")))) {
            codigos.sumarIntento(codigoId);
            throw new IllegalArgumentException("El código no es correcto");
        }
        codigos.usar(codigoId);
        return emitirSesion(usuario, perfil, parametro);
    }

    @Override
    public Map<String, Object> dosFactoresEstado(ContextoAutenticacion contexto) {
        Usuario usuario = datos.findUsuarioById(contexto.usuarioId());
        Map<String, Object> perfil = perfil(usuario);
        String destino = perfil == null ? null : texto(perfil.get("correo"));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("activo", Boolean.TRUE.equals(usuario.getDosFactores()));
        r.put("tieneCorreo", !vacio(destino));
        r.put("servicioDisponible", correo.configurado());
        r.put("correo", vacio(destino) ? null : ocultarCorreo(destino));
        return r;
    }

    @Override
    public Map<String, Object> activarDosFactoresEnviar(ContextoAutenticacion contexto) {
        Usuario usuario = datos.findUsuarioById(contexto.usuarioId());
        Map<String, Object> perfil = perfil(usuario);
        String destino = perfil == null ? null : texto(perfil.get("correo"));
        if (vacio(destino)) throw new IllegalStateException("Agrega un correo a tu perfil para poder activar la verificación en dos pasos");
        if (!correo.configurado()) throw new IllegalStateException("No hay un servicio de correo disponible");
        String codigo = generarCodigo();
        codigos.crear(usuario.getUsuarioId(), ACTIVACION_2FA, passwordEncoder.encode(codigo), Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));
        boolean enviado = correo.enviar(destino, "Código para activar la verificación en dos pasos",
                correo.plantillaCodigo("Activa la verificación en dos pasos",
                        "Usa este código para activar la verificación en dos pasos de tu cuenta:", codigo, "El código vence en 15 minutos."));
        if (!enviado) throw new IllegalStateException("No se pudo enviar el código a tu correo");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("correo", ocultarCorreo(destino));
        return r;
    }

    @Override
    public void activarDosFactoresConfirmar(ContextoAutenticacion contexto, AutenticacionDosFactoresParameters parametro) {
        if (parametro == null || vacio(parametro.getCodigo())) throw new IllegalArgumentException("El código es requerido");
        Map<String, Object> codigo = codigos.vigente(contexto.usuarioId(), ACTIVACION_2FA);
        if (codigo == null) throw new IllegalArgumentException("El código expiró o no es válido");
        Long codigoId = ((Number) codigo.get("codigoVerificacionId")).longValue();
        if (!passwordEncoder.matches(parametro.getCodigo().trim().toUpperCase(), String.valueOf(codigo.get("codigoHash")))) {
            codigos.sumarIntento(codigoId);
            throw new IllegalArgumentException("El código no es correcto");
        }
        codigos.usar(codigoId);
        datos.actualizarDosFactores(contexto.usuarioId(), true);
    }

    @Override
    public void desactivarDosFactores(ContextoAutenticacion contexto, AutenticacionDosFactoresParameters parametro) {
        if (parametro == null || vacio(parametro.getContrasena())) throw new IllegalArgumentException("La contraseña es requerida");
        Usuario usuario = datos.findUsuarioById(contexto.usuarioId());
        if (usuario == null || !passwordEncoder.matches(parametro.getContrasena(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña no es correcta");
        }
        datos.actualizarDosFactores(contexto.usuarioId(), false);
    }

    private void enviarCodigoIngreso(Usuario usuario, String destino) {
        String codigo = generarCodigo();
        codigos.crear(usuario.getUsuarioId(), INGRESO_2FA, passwordEncoder.encode(codigo), Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
        correo.enviar(destino, "Tu código de acceso",
                correo.plantillaCodigo("Código de acceso",
                        "Usa este código para completar el inicio de sesión:", codigo, "El código vence en 10 minutos. Si no fuiste tú, cambia tu contraseña."));
    }

    private Map<String, Object> emitirSesion(Usuario usuario, Map<String, Object> perfil, AutenticacionLoginParameters parametro) {
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

    private Map<String, Object> validarEstado(Usuario usuario) {
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
        return perfil;
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

    private String generarCodigo() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    private String ocultarCorreo(String correoValor) {
        if (vacio(correoValor) || !correoValor.contains("@")) return correoValor;
        String[] partes = correoValor.split("@", 2);
        String local = partes[0];
        String visible = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
        return visible + "@" + partes[1];
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }

    private Long numero(Object valor) {
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
