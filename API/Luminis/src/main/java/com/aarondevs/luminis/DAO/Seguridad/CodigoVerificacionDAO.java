package com.aarondevs.luminis.DAO.Seguridad;

import java.sql.Timestamp;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CodigoVerificacionDAO implements CodigoVerificacionDataSet {
    private final JdbcTemplate jdbc;

    public CodigoVerificacionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void crear(Long usuarioId, String proposito, String codigoHash, Timestamp expira) {
        jdbc.update("UPDATE CodigosVerificacion SET usado=TRUE WHERE usuarioId=? AND proposito=? AND usado=FALSE", usuarioId, proposito);
        jdbc.update("INSERT INTO CodigosVerificacion (usuarioId, proposito, codigoHash, expira) VALUES (?, ?, ?, ?)",
                usuarioId, proposito, codigoHash, expira);
    }

    @Override
    public Map<String, Object> vigente(Long usuarioId, String proposito) {
        return jdbc.queryForList("SELECT codigoVerificacionId, codigoHash, intentos FROM CodigosVerificacion "
                        + "WHERE usuarioId=? AND proposito=? AND usado=FALSE AND expira>NOW(3) ORDER BY codigoVerificacionId DESC LIMIT 1",
                usuarioId, proposito).stream().findFirst().orElse(null);
    }

    @Override
    public void usar(Long codigoVerificacionId) {
        jdbc.update("UPDATE CodigosVerificacion SET usado=TRUE WHERE codigoVerificacionId=?", codigoVerificacionId);
    }

    @Override
    public void sumarIntento(Long codigoVerificacionId) {
        jdbc.update("UPDATE CodigosVerificacion SET intentos=intentos+1, usado=IF(intentos+1>=5, TRUE, usado) WHERE codigoVerificacionId=?", codigoVerificacionId);
    }
}
