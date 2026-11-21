package com.aarondevs.luminis.DAO.Seguridad;

public interface PermisoDataSet {
    Long sincronizarModulo(String area, String clave, String nombre, String descripcion, String icono, Integer orden,
                           String categoria, String categoriaNombre, String categoriaIcono, Integer categoriaOrden);

    void sincronizarAccion(String area, Long moduloId, String clave, String nombre, Integer orden);

    Long asegurarRolAdministracionProtegido(String nombre);

    void otorgarAccionesARolesProtegidos();

    Long findUsuarioIdByUsuario(String usuario);

    Long crearUsuario(String usuario, String contrasenaHash, String tipo, boolean debeCambiarContrasena, String actor);

    Long crearUsuarioAdministracion(Long usuarioId, String nombres, String apellidos, String correo, String actor);

    void asignarRolAdministracion(Long usuarioAdministracionId, Long rolAdministracionId, String actor);
}
