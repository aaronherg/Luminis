package com.aarondevs.luminis.DAO.Seguridad;

public interface PermisoDataSet {
    Long asegurarRolAdministracionProtegido(String nombre);

    Long findUsuarioIdByUsuario(String usuario);

    Long crearUsuario(String usuario, String contrasenaHash, String tipo, boolean debeCambiarContrasena, String actor);

    Long crearUsuarioAdministracion(Long usuarioId, String nombres, String apellidos, String correo, String actor);

    void asignarRolAdministracion(Long usuarioAdministracionId, Long rolAdministracionId, String actor);
}
