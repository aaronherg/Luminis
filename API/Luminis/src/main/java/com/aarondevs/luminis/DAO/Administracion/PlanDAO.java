package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.Plan;
import com.aarondevs.luminis.Parameters.Administracion.PlanActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanAgregarParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanDAO implements PlanDataSet {
    private static final String SELECT = "SELECT p.*, m.simbolo AS monedaSimbolo, m.codigo AS monedaCodigo "
            + "FROM Planes p JOIN Monedas m ON m.monedaId=p.monedaId ";

    private final JdbcTemplate jdbc;

    public PlanDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Plan create(PlanAgregarParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        String monedaUuid = jdbc.queryForObject("SELECT monedaUuid FROM Monedas WHERE monedaId=?", String.class, parametro.getMonedaId());
        jdbc.update("INSERT INTO Planes (planUuid, monedaId, monedaUuid, nombre, descripcion, modalidad, peticionesMes, conexionesSimultaneas, "
                        + "tamanoMaximoMensajeKb, retencionSegundos, precio, precioMilAdicional, caracteristicas, destacado, publico, orden, usuarioCreacionUuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuid, parametro.getMonedaId(), monedaUuid, parametro.getNombre().trim(), parametro.getDescripcion(), parametro.getModalidad(),
                parametro.getPeticionesMes(), parametro.getConexionesSimultaneas(), parametro.getTamanoMaximoMensajeKb(),
                parametro.getRetencionSegundos() == null ? 10 : parametro.getRetencionSegundos(), parametro.getPrecio(), parametro.getPrecioMilAdicional(),
                parametro.getCaracteristicas(), Boolean.TRUE.equals(parametro.getDestacado()), !Boolean.FALSE.equals(parametro.getPublico()),
                parametro.getOrden() == null ? 0 : parametro.getOrden(), actor);
        return findByUuid(uuid);
    }

    @Override
    public Plan update(PlanActualizarParameters parametro, String actor) {
        String monedaUuid = jdbc.queryForObject("SELECT monedaUuid FROM Monedas WHERE monedaId=?", String.class, parametro.getMonedaId());
        int actualizados = jdbc.update("UPDATE Planes SET monedaId=?, monedaUuid=?, nombre=?, descripcion=?, modalidad=?, peticionesMes=?, "
                        + "conexionesSimultaneas=?, tamanoMaximoMensajeKb=?, retencionSegundos=?, precio=?, precioMilAdicional=?, caracteristicas=?, destacado=?, publico=?, orden=?, "
                        + "estado=COALESCE(NULLIF(?,''),estado), fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                        + "usuarioModificacionUuid=? WHERE planUuid=? AND estado<>'Eliminado'",
                parametro.getMonedaId(), monedaUuid, parametro.getNombre().trim(), parametro.getDescripcion(), parametro.getModalidad(),
                parametro.getPeticionesMes(), parametro.getConexionesSimultaneas(), parametro.getTamanoMaximoMensajeKb(),
                parametro.getRetencionSegundos() == null ? 10 : parametro.getRetencionSegundos(), parametro.getPrecio(), parametro.getPrecioMilAdicional(),
                parametro.getCaracteristicas(), Boolean.TRUE.equals(parametro.getDestacado()), !Boolean.FALSE.equals(parametro.getPublico()),
                parametro.getOrden() == null ? 0 : parametro.getOrden(), parametro.getEstado(), actor, parametro.getPlanUuid());
        return actualizados == 0 ? null : findByUuid(parametro.getPlanUuid());
    }

    @Override
    public Plan findById(Long id) {
        return jdbc.query(SELECT + "WHERE p.planId=? AND p.estado<>'Eliminado'", new BeanPropertyRowMapper<>(Plan.class), id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public Plan findByUuid(String uuid) {
        return jdbc.query(SELECT + "WHERE p.planUuid=?", new BeanPropertyRowMapper<>(Plan.class), uuid).stream().findFirst().orElse(null);
    }

    @Override
    public List<Plan> findAll() {
        return jdbc.query(SELECT + "WHERE p.estado<>'Eliminado' ORDER BY p.orden, p.precio", new BeanPropertyRowMapper<>(Plan.class));
    }

    @Override
    public List<Plan> findPublicos() {
        return jdbc.query(SELECT + "WHERE p.estado='Activo' AND p.publico=TRUE ORDER BY p.orden, p.precio", new BeanPropertyRowMapper<>(Plan.class));
    }

    @Override
    public boolean existsMoneda(Long monedaId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Monedas WHERE monedaId=? AND estado='Activo'", Integer.class, monedaId);
        return total != null && total > 0;
    }

    @Override
    public boolean enUso(Long id) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM DetallesSuscripciones WHERE planId=? AND estado='Activo' AND fechaFin>NOW(3)",
                Integer.class, id);
        return total != null && total > 0;
    }

    @Override
    public boolean delete(Long id, String actor) {
        return jdbc.update("UPDATE Planes SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE planId=? AND estado<>'Eliminado'", actor, id) > 0;
    }
}
