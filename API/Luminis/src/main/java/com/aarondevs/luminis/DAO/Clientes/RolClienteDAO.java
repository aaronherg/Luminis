package com.aarondevs.luminis.DAO.Clientes;

import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaClienteDAO;
import com.aarondevs.luminis.Models.Clientes.RolCliente;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteAgregarParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RolClienteDAO implements RolClienteDataSet {
    private final JdbcTemplate jdbc;
    private final PermisoJerarquiaClienteDAO permisos;

    public RolClienteDAO(JdbcTemplate jdbc, PermisoJerarquiaClienteDAO permisos) {
        this.jdbc = jdbc;
        this.permisos = permisos;
    }

    @Override
    public RolCliente create(RolClienteAgregarParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        String clienteUuid = jdbc.queryForObject("SELECT clienteUuid FROM Clientes WHERE clienteId=?", String.class, parametro.getClienteId());
        jdbc.update("INSERT INTO RolesClientes (rolClienteUuid, clienteId, clienteUuid, nombre, descripcion, color, protegido, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)", uuid, parametro.getClienteId(), clienteUuid, parametro.getNombre().trim(), parametro.getDescripcion(),
                parametro.getColor(), actor);
        RolCliente creado = findByUuid(uuid);
        guardarPermisos(creado.getRolClienteId(), parametro, actor);
        return findByUuid(uuid);
    }

    @Override
    public RolCliente update(RolClienteActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE RolesClientes SET nombre=?, descripcion=?, color=?, estado=COALESCE(NULLIF(?,''),estado), "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                        + "WHERE rolClienteUuid=? AND estado<>'Eliminado'",
                parametro.getNombre().trim(), parametro.getDescripcion(), parametro.getColor(), parametro.getEstado(), actor, parametro.getRolClienteUuid());
        if (actualizados == 0) return null;
        RolCliente actualizado = findByUuid(parametro.getRolClienteUuid());
        if (!Boolean.TRUE.equals(actualizado.getProtegido())) guardarPermisos(actualizado.getRolClienteId(), parametro, actor);
        return findByUuid(parametro.getRolClienteUuid());
    }

    @Override
    public RolCliente findById(Long id) {
        return completar(jdbc.query("SELECT * FROM RolesClientes WHERE rolClienteId=? AND estado<>'Eliminado'",
                new BeanPropertyRowMapper<>(RolCliente.class), id).stream().findFirst().orElse(null));
    }

    @Override
    public RolCliente findByUuid(String uuid) {
        return completar(jdbc.query("SELECT * FROM RolesClientes WHERE rolClienteUuid=?",
                new BeanPropertyRowMapper<>(RolCliente.class), uuid).stream().findFirst().orElse(null));
    }

    @Override
    public List<RolCliente> findAll(Long clienteId) {
        List<RolCliente> roles = jdbc.query("SELECT r.* FROM RolesClientes r WHERE r.clienteId=? AND r.estado<>'Eliminado' ORDER BY r.protegido DESC, r.nombre",
                new BeanPropertyRowMapper<>(RolCliente.class), clienteId);
        roles.forEach(this::completar);
        return roles;
    }

    @Override
    public boolean existsNombre(Long clienteId, String nombre, String excluirUuid) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM RolesClientes WHERE clienteId=? AND LOWER(nombre)=LOWER(?) "
                + "AND estado<>'Eliminado' AND rolClienteUuid<>COALESCE(?,'')", Integer.class, clienteId, nombre.trim(), excluirUuid);
        return total != null && total > 0;
    }

    @Override
    public boolean enUso(Long id) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM UsuariosClientesRoles WHERE rolClienteId=? AND estado='Activo'", Integer.class, id);
        return total != null && total > 0;
    }

    private void guardarPermisos(Long rolId, RolClienteAgregarParameters parametro, String actor) {
        permisos.guardarPermisos(rolId, parametro.getModulosIds(), parametro.getCategoriasIds(), parametro.getOpcionesIds(), parametro.getAccionesIds(), actor);
    }

    @Override
    public boolean delete(Long id, String actor) {
        boolean eliminado = jdbc.update("UPDATE RolesClientes SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE rolClienteId=? AND estado<>'Eliminado' AND protegido=FALSE", actor, id) > 0;
        if (eliminado) permisos.revocarPermisos(id, actor);
        return eliminado;
    }

    @Override
    public List<java.util.Map<String, Object>> modulos() {
        return permisos.arbol();
    }

    private RolCliente completar(RolCliente rol) {
        if (rol != null) {
            java.util.Map<String, List<Long>> p = permisos.permisosRol(rol.getRolClienteId());
            rol.setModulosIds(p.get("modulosIds"));
            rol.setCategoriasIds(p.get("categoriasIds"));
            rol.setOpcionesIds(p.get("opcionesIds"));
            rol.setAccionesIds(p.get("accionesIds"));
        }
        return rol;
    }
}
