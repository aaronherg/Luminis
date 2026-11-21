package com.aarondevs.luminis.DAO.Mensajeria;

import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApiKeyDAO implements ApiKeyDataSet {
    private final JdbcTemplate jdbc;

    public ApiKeyDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ApiKey> findAll(Long clienteId) {
        return jdbc.query("SELECT * FROM ApiKeys WHERE clienteId=? AND estado<>'Eliminado' ORDER BY fechaCreacion DESC",
                new BeanPropertyRowMapper<>(ApiKey.class), clienteId);
    }

    @Override
    public ApiKey findById(Long apiKeyId) {
        return jdbc.query("SELECT * FROM ApiKeys WHERE apiKeyId=? AND estado<>'Eliminado'", new BeanPropertyRowMapper<>(ApiKey.class), apiKeyId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public ApiKey findActivaByHash(String hash) {
        return jdbc.query("SELECT k.* FROM ApiKeys k JOIN Clientes c ON c.clienteId=k.clienteId "
                        + "WHERE k.hash=? AND k.estado='Activo' AND c.estado='Activo'",
                new BeanPropertyRowMapper<>(ApiKey.class), hash).stream().findFirst().orElse(null);
    }

    @Override
    public Long create(Long clienteId, String nombre, String prefijo, String hash, String permisos, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO ApiKeys (apiKeyUuid, clienteId, clienteUuid, nombre, prefijo, hash, permisos, usuarioCreacionUuid) "
                        + "SELECT ?, c.clienteId, c.clienteUuid, ?, ?, ?, ?, ? FROM Clientes c WHERE c.clienteId=?",
                uuid, nombre, prefijo, hash, permisos, actor, clienteId);
        return jdbc.queryForObject("SELECT apiKeyId FROM ApiKeys WHERE apiKeyUuid=?", Long.class, uuid);
    }

    @Override
    public int update(Long apiKeyId, String nombre, String permisos, String estado, String actor) {
        return jdbc.update("UPDATE ApiKeys SET nombre=?, permisos=?, estado=?, fechaModificacion=NOW(3), fechaVersion=NOW(3), "
                + "version=version+1, usuarioModificacionUuid=? WHERE apiKeyId=? AND estado<>'Eliminado'", nombre, permisos, estado, actor, apiKeyId);
    }

    @Override
    public int revocar(Long apiKeyId, String actor) {
        return jdbc.update("UPDATE ApiKeys SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE apiKeyId=? AND estado<>'Eliminado'", actor, apiKeyId);
    }

    @Override
    public void registrarUso(Long apiKeyId) {
        jdbc.update("UPDATE ApiKeys SET fechaUltimoUso=NOW(3) WHERE apiKeyId=?", apiKeyId);
    }

    @Override
    public long countActivas(Long clienteId) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM ApiKeys WHERE clienteId=? AND estado='Activo'", Long.class, clienteId);
        return total == null ? 0 : total;
    }
}
