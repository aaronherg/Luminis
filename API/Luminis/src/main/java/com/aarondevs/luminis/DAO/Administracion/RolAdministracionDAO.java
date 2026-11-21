package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.ModuloAdministracion;
import com.aarondevs.luminis.Models.Administracion.ModuloAdministracionAccion;
import com.aarondevs.luminis.Models.Administracion.RolAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionAgregarParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RolAdministracionDAO implements RolAdministracionDataSet {
    private final JdbcTemplate jdbc;

    public RolAdministracionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RolAdministracion create(RolAdministracionAgregarParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO RolesAdministracion (rolAdministracionUuid, nombre, descripcion, color, protegido, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, FALSE, ?)", uuid, parametro.getNombre().trim(), parametro.getDescripcion(),
                parametro.getColor(), actor);
        RolAdministracion creado = findByUuid(uuid);
        reemplazarAcciones(creado.getRolAdministracionId(), parametro.getAccionesIds(), actor);
        return findByUuid(uuid);
    }

    @Override
    public RolAdministracion update(RolAdministracionActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE RolesAdministracion SET nombre=?, descripcion=?, color=?, estado=COALESCE(NULLIF(?,''),estado), "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                        + "WHERE rolAdministracionUuid=? AND estado<>'Eliminado'",
                parametro.getNombre().trim(), parametro.getDescripcion(), parametro.getColor(), parametro.getEstado(), actor, parametro.getRolAdministracionUuid());
        if (actualizados == 0) return null;
        RolAdministracion actualizado = findByUuid(parametro.getRolAdministracionUuid());
        if (!Boolean.TRUE.equals(actualizado.getProtegido())) reemplazarAcciones(actualizado.getRolAdministracionId(), parametro.getAccionesIds(), actor);
        return findByUuid(parametro.getRolAdministracionUuid());
    }

    @Override
    public RolAdministracion findById(Long id) {
        return completar(jdbc.query("SELECT * FROM RolesAdministracion WHERE rolAdministracionId=? AND estado<>'Eliminado'",
                new BeanPropertyRowMapper<>(RolAdministracion.class), id).stream().findFirst().orElse(null));
    }

    @Override
    public RolAdministracion findByUuid(String uuid) {
        return completar(jdbc.query("SELECT * FROM RolesAdministracion WHERE rolAdministracionUuid=?",
                new BeanPropertyRowMapper<>(RolAdministracion.class), uuid).stream().findFirst().orElse(null));
    }

    @Override
    public List<RolAdministracion> findAll() {
        List<RolAdministracion> roles = jdbc.query("SELECT r.* FROM RolesAdministracion r WHERE r.estado<>'Eliminado' ORDER BY r.protegido DESC, r.nombre",
                new BeanPropertyRowMapper<>(RolAdministracion.class));
        roles.forEach(this::completar);
        return roles;
    }

    @Override
    public boolean existsNombre(String nombre, String excluirUuid) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM RolesAdministracion WHERE LOWER(nombre)=LOWER(?) "
                + "AND estado<>'Eliminado' AND rolAdministracionUuid<>COALESCE(?,'')", Integer.class, nombre.trim(), excluirUuid);
        return total != null && total > 0;
    }

    @Override
    public boolean enUso(Long id) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM UsuariosAdministracionRoles WHERE rolAdministracionId=? AND estado='Activo'", Integer.class, id);
        return total != null && total > 0;
    }

    @Override
    public void reemplazarAcciones(Long rolAdministracionId, List<Long> accionesIds, String actor) {
        jdbc.update("DELETE FROM RolesAdministracionAcciones WHERE rolAdministracionId=?", rolAdministracionId);
        if (accionesIds == null) return;
        for (Long accionId : accionesIds.stream().distinct().toList()) {
            jdbc.update("INSERT IGNORE INTO RolesAdministracionAcciones (rolAdministracionAccionUuid, rolAdministracionId, rolAdministracionUuid, moduloAdministracionAccionId, moduloAdministracionAccionUuid, usuarioCreacionUuid) "
                    + "SELECT UUID(), r.rolAdministracionId, r.rolAdministracionUuid, a.moduloAdministracionAccionId, a.moduloAdministracionAccionUuid, ? FROM RolesAdministracion r CROSS JOIN ModulosAdministracionAcciones a "
                    + "WHERE r.rolAdministracionId=? AND a.moduloAdministracionAccionId=? AND a.estado='Activo'", actor, rolAdministracionId, accionId);
        }
    }

    @Override
    public boolean delete(Long id, String actor) {
        return jdbc.update("UPDATE RolesAdministracion SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE rolAdministracionId=? AND estado<>'Eliminado' AND protegido=FALSE", actor, id) > 0;
    }

    @Override
    public List<ModuloAdministracion> modulos() {
        List<ModuloAdministracion> modulos = jdbc.query("SELECT * FROM ModulosAdministracion WHERE estado='Activo' ORDER BY categoriaOrden, orden, moduloAdministracionId",
                new BeanPropertyRowMapper<>(ModuloAdministracion.class));
        for (ModuloAdministracion modulo : modulos) {
            modulo.setAcciones(jdbc.query("SELECT * FROM ModulosAdministracionAcciones WHERE moduloAdministracionId=? AND estado='Activo' ORDER BY orden, moduloAdministracionAccionId",
                    new BeanPropertyRowMapper<>(ModuloAdministracionAccion.class), modulo.getModuloAdministracionId()));
        }
        return modulos;
    }

    private RolAdministracion completar(RolAdministracion rol) {
        if (rol != null) {
            rol.setAccionesIds(jdbc.queryForList("SELECT moduloAdministracionAccionId FROM RolesAdministracionAcciones WHERE rolAdministracionId=? AND estado='Activo'",
                    Long.class, rol.getRolAdministracionId()));
        }
        return rol;
    }
}
