package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.DAO.Seguridad.PermisoDataSet;
import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaAdministracionDAO;
import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaClienteDAO;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class CatalogoPermisosInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CatalogoPermisosInitializer.class);

    private final PermisoDataSet datos;
    private final PermisoJerarquiaAdministracionDAO permisosAdministracion;
    private final PermisoJerarquiaClienteDAO permisosClientes;
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

    public CatalogoPermisosInitializer(PermisoDataSet datos, PermisoJerarquiaAdministracionDAO permisosAdministracion, PermisoJerarquiaClienteDAO permisosClientes,
                                       ResourceLoader resourceLoader, PasswordEncoder passwordEncoder) {
        this.datos = datos;
        this.permisosAdministracion = permisosAdministracion;
        this.permisosClientes = permisosClientes;
        this.resourceLoader = resourceLoader;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        SincronizadorPermisos.sincronizar(resourceLoader, catalogoAdministracion, permisosAdministracion);
        SincronizadorPermisos.sincronizar(resourceLoader, catalogoClientes, permisosClientes);
        Long rolAdministrador = datos.asegurarRolAdministracionProtegido(adminRol);
        permisosAdministracion.otorgarProtegidos();
        permisosClientes.otorgarProtegidos();
        asegurarAdministradorInicial(rolAdministrador);
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
}
