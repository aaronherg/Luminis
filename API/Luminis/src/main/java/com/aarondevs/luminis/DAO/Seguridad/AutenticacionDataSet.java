package com.aarondevs.luminis.DAO.Seguridad;

import com.aarondevs.luminis.Models.Seguridad.Usuario;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface AutenticacionDataSet {
    Usuario findUsuarioByUsuario(String usuario);

    Usuario findUsuarioById(Long usuarioId);

    Map<String, Object> perfilAdministracion(Long usuarioId);

    Map<String, Object> perfilCliente(Long usuarioId);

    void crear(Long usuarioId, String usuarioUuid, String tipo, String jti, String ip, String agente, Timestamp expiracion);

    void registrarAcceso(Long usuarioId);

    boolean cerrar(String jti, String motivo);

    void actualizarContrasena(Long usuarioId, String contrasenaHash, String actor);

    List<Map<String, Object>> modulos(String area);
}
