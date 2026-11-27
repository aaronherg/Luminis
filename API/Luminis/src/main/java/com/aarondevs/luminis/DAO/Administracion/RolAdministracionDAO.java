package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaAdministracionDAO;
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
    private final PermisoJerarquiaAdministracionDAO permisos;

    public RolAdministracionDAO(JdbcTemplate jdbc, PermisoJerarquiaAdministracionDAO permisos) {
        this.jdbc = jdbc;
        this.permisos = permisos;
    }

    @Override
    public RolAdministracion create(RolAdministracionAgregarParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO RolesAdministracion (rolAdministracionUuid, nombre, descripcion, color, protegido, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, FALSE, ?)", uuid, parametro.getNombre().trim(), parametro.getDescripcion(),
                parametro.getColor(), actor);
        RolAdministracion creado = findByUuid(uuid);
        guardarPermisos(creado.getRolAdministracionId(), parametro, actor);
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
        if (!Boolean.TRUE.equals(actualizado.getProtegido())) guardarPermisos(actualizado.getRolAdministracionId(), parametro, actor);
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

    private void guardarPermisos(Long rolId, RolAdministracionAgregarParameters parametro, String actor) {
        permisos.guardarPermisos(rolId, parametro.getModulosIds(), parametro.getCategoriasIds(), parametro.getOpcionesIds(), parametro.getAccionesIds(), actor);
    }

    @Override
    public boolean delete(Long id, String actor) {
        boolean eliminado = jdbc.update("UPDATE RolesAdministracion SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE rolAdministracionId=? AND estado<>'Eliminado' AND protegido=FALSE", actor, id) > 0;
        if (eliminado) permisos.revocarPermisos(id, actor);
        return eliminado;
    }

    @Override
    public List<java.util.Map<String, Object>> modulos() {
        return permisos.arbol();
    }

    private RolAdministracion completar(RolAdministracion rol) {
        if (rol != null) {
            java.util.Map<String, List<Long>> p = permisos.permisosRol(rol.getRolAdministracionId());
            rol.setModulosIds(p.get("modulosIds"));
            rol.setCategoriasIds(p.get("categoriasIds"));
            rol.setOpcionesIds(p.get("opcionesIds"));
            rol.setAccionesIds(p.get("accionesIds"));
        }
        return rol;
    }
}
