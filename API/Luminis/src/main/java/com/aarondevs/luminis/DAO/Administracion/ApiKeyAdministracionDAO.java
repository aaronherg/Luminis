package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApiKeyAdministracionDAO implements ApiKeyAdministracionDataSet {
    private final JdbcTemplate jdbc;

    public ApiKeyAdministracionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Map<String, Object>> listar(Long usuarioAdministracionId) {
        return jdbc.queryForList("SELECT apiKeyAdministracionUuid AS uuid, nombre, prefijo, estado, fechaCreacion AS fechaGeneracion, fechaUltimoUso "
                + "FROM ApiKeysAdministracion WHERE usuarioAdministracionId=? AND estado<>'Eliminada' ORDER BY fechaCreacion DESC", usuarioAdministracionId);
    }

    @Override
    public Map<String, Object> porUuid(String uuid, Long usuarioAdministracionId) {
        return jdbc.queryForList("SELECT apiKeyAdministracionUuid AS uuid, nombre, prefijo, estado FROM ApiKeysAdministracion "
                + "WHERE apiKeyAdministracionUuid=? AND usuarioAdministracionId=? AND estado<>'Eliminada'", uuid, usuarioAdministracionId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void crear(String uuid, Long usuarioAdministracionId, String nombre, String prefijo, String hash, String actor) {
        jdbc.update("INSERT INTO ApiKeysAdministracion (apiKeyAdministracionUuid, usuarioAdministracionId, nombre, prefijo, apiKeyHash, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?)", uuid, usuarioAdministracionId, nombre, prefijo, hash, actor);
    }

    @Override
    public int renombrar(String uuid, Long usuarioAdministracionId, String nombre, String actor) {
        return jdbc.update("UPDATE ApiKeysAdministracion SET nombre=?, fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                + "WHERE apiKeyAdministracionUuid=? AND usuarioAdministracionId=? AND estado<>'Eliminada'", nombre, actor, uuid, usuarioAdministracionId);
    }

    @Override
    public int cambiarEstado(String uuid, Long usuarioAdministracionId, String estado, String actor) {
        return jdbc.update("UPDATE ApiKeysAdministracion SET estado=?, fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                + "WHERE apiKeyAdministracionUuid=? AND usuarioAdministracionId=? AND estado<>'Eliminada'", estado, actor, uuid, usuarioAdministracionId);
    }

    @Override
    public ContextoAutenticacion resolver(String hash) {
        return jdbc.query("SELECT u.usuarioId, u.usuarioUuid, ua.usuarioAdministracionId, ua.usuarioAdministracionUuid "
                        + "FROM ApiKeysAdministracion k "
                        + "JOIN UsuariosAdministracion ua ON ua.usuarioAdministracionId=k.usuarioAdministracionId AND ua.estado='Activo' "
                        + "JOIN Usuarios u ON u.usuarioId=ua.usuarioId AND u.estado='Activo' "
                        + "WHERE k.apiKeyHash=? AND k.estado='Activa' LIMIT 1",
                (rs, i) -> new ContextoAutenticacion(rs.getLong("usuarioId"), rs.getString("usuarioUuid"),
                        ContextoAutenticacion.ADMINISTRACION, null, null, null, null, null,
                        rs.getLong("usuarioAdministracionId"), rs.getString("usuarioAdministracionUuid")),
                hash).stream().findFirst().orElse(null);
    }

    @Override
    public void registrarUso(String hash) {
        jdbc.update("UPDATE ApiKeysAdministracion SET fechaUltimoUso=NOW(3) WHERE apiKeyHash=?", hash);
    }
}
