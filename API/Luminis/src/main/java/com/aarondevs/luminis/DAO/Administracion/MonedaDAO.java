package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.Moneda;
import com.aarondevs.luminis.Parameters.Administracion.MonedaActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaAgregarParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MonedaDAO implements MonedaDataSet {
    private final JdbcTemplate jdbc;

    public MonedaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Moneda create(MonedaAgregarParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO Monedas (monedaUuid, codigo, nombre, simbolo, tasa, usuarioCreacionUuid) VALUES (?, ?, ?, ?, ?, ?)",
                uuid, parametro.getCodigo().trim().toUpperCase(), parametro.getNombre().trim(), parametro.getSimbolo().trim(),
                parametro.getTasa(), actor);
        return findByUuid(uuid);
    }

    @Override
    public Moneda update(MonedaActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE Monedas SET codigo=?, nombre=?, simbolo=?, tasa=?, estado=COALESCE(NULLIF(?,''),estado), "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                        + "WHERE monedaUuid=? AND estado<>'Eliminado'",
                parametro.getCodigo().trim().toUpperCase(), parametro.getNombre().trim(), parametro.getSimbolo().trim(),
                parametro.getTasa(), parametro.getEstado(), actor, parametro.getMonedaUuid());
        return actualizados == 0 ? null : findByUuid(parametro.getMonedaUuid());
    }

    @Override
    public Moneda findById(Long id) {
        return jdbc.query("SELECT * FROM Monedas WHERE monedaId=? AND estado<>'Eliminado'",
                new BeanPropertyRowMapper<>(Moneda.class), id).stream().findFirst().orElse(null);
    }

    @Override
    public Moneda findByUuid(String uuid) {
        return jdbc.query("SELECT * FROM Monedas WHERE monedaUuid=?",
                new BeanPropertyRowMapper<>(Moneda.class), uuid).stream().findFirst().orElse(null);
    }

    @Override
    public List<Moneda> findAll() {
        return jdbc.query("SELECT * FROM Monedas WHERE estado<>'Eliminado' ORDER BY nombre", new BeanPropertyRowMapper<>(Moneda.class));
    }

    @Override
    public boolean existsCodigo(String codigo, String excluirUuid) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Monedas WHERE codigo=? AND estado<>'Eliminado' AND monedaUuid<>COALESCE(?,'')",
                Integer.class, codigo.trim().toUpperCase(), excluirUuid);
        return total != null && total > 0;
    }

    @Override
    public boolean enUso(Long id) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Planes WHERE monedaId=? AND estado<>'Eliminado'", Integer.class, id);
        return total != null && total > 0;
    }

    @Override
    public boolean delete(Long id, String actor) {
        return jdbc.update("UPDATE Monedas SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE monedaId=? AND estado<>'Eliminado'", actor, id) > 0;
    }
}
