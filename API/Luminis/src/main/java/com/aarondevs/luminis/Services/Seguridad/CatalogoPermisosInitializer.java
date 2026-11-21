package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.DAO.Seguridad.PermisoDataSet;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class CatalogoPermisosInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CatalogoPermisosInitializer.class);

    private final PermisoDataSet datos;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.luminis.permisos.administracion}")
    private String catalogoAdministracion;
    @Value("${app.luminis.permisos.clientes}")
    private String catalogoClientes;
    @Value("${app.luminis.admin.usuario}")
    private String adminUsuario;
    @Value("${app.luminis.admin.contrasena}")
    private String adminContrasena;
    @Value("${app.luminis.admin.nombres}")
    private String adminNombres;
    @Value("${app.luminis.admin.apellidos}")
    private String adminApellidos;
    @Value("${app.luminis.admin.correo}")
    private String adminCorreo;
    @Value("${app.luminis.admin.rol}")
    private String adminRol;
    @Value("${app.luminis.admin.cambiar-contrasena}")
    private boolean adminCambiarContrasena;

    public CatalogoPermisosInitializer(PermisoDataSet datos, ResourceLoader resourceLoader, PasswordEncoder passwordEncoder) {
        this.datos = datos;
        this.resourceLoader = resourceLoader;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        sincronizar("Administracion", catalogoAdministracion);
        sincronizar("Clientes", catalogoClientes);
        Long rolAdministrador = datos.asegurarRolAdministracionProtegido(adminRol);
        datos.otorgarAccionesARolesProtegidos();
        asegurarAdministradorInicial(rolAdministrador);
    }

    private void sincronizar(String area, String ubicacion) throws Exception {
        Resource recurso = resourceLoader.getResource(ubicacion);
        if (!recurso.exists()) throw new IllegalStateException("No se encontró el catálogo de permisos: " + ubicacion);
        try (InputStream entrada = recurso.getInputStream()) {
            JsonNode categorias = objectMapper.readTree(entrada).path("categorias");
            if (!categorias.isArray()) throw new IllegalStateException("El catálogo " + ubicacion + " debe incluir una lista 'categorias'");
            for (JsonNode categoria : categorias) {
                String categoriaClave = requerido(categoria, "clave");
                String categoriaNombre = categoria.path("sola").asBoolean(false) ? null : requerido(categoria, "nombre");
                for (JsonNode modulo : categoria.path("opciones")) {
                    Long moduloId = datos.sincronizarModulo(area, requerido(modulo, "clave"), requerido(modulo, "nombre"),
                            modulo.path("descripcion").asText(null), modulo.path("icono").asText(null), modulo.path("orden").asInt(0),
                            categoriaClave, categoriaNombre, categoria.path("icono").asText(null), categoria.path("orden").asInt(0));
                    int orden = 0;
                    for (JsonNode accion : modulo.path("acciones")) {
                        datos.sincronizarAccion(area, moduloId, requerido(accion, "clave"), requerido(accion, "nombre"), ++orden);
                    }
                }
            }
        }
    }

    private void asegurarAdministradorInicial(Long rolAdministrador) {
        if (adminUsuario == null || adminUsuario.isBlank() || datos.findUsuarioIdByUsuario(adminUsuario.trim()) != null) return;
        if (adminContrasena == null || adminContrasena.isBlank()) {
            throw new IllegalStateException("LUMINIS_ADMIN_CONTRASENA es requerida para crear el usuario administrador inicial");
        }
        Long usuarioId = datos.crearUsuario(adminUsuario.trim(), passwordEncoder.encode(adminContrasena),
                ContextoAutenticacion.ADMINISTRACION, adminCambiarContrasena, "SISTEMA");
        Long usuarioAdministracionId = datos.crearUsuarioAdministracion(usuarioId, adminNombres, adminApellidos, adminCorreo, "SISTEMA");
        datos.asignarRolAdministracion(usuarioAdministracionId, rolAdministrador, "SISTEMA");
        log.info("Usuario administrador inicial '{}' creado.", adminUsuario);
    }

    private String requerido(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        if (valor == null || valor.isBlank()) throw new IllegalStateException("El catálogo de permisos tiene un elemento sin '" + campo + "'");
        return valor;
    }
}
