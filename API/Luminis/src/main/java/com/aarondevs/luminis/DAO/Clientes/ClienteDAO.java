package com.aarondevs.luminis.DAO.Clientes;

import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaClienteDAO;
import com.aarondevs.luminis.Models.Clientes.Cliente;
import com.aarondevs.luminis.Parameters.Clientes.ClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteDatosParameters;
import com.aarondevs.luminis.Parameters.Response.PaginationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteDAO implements ClienteDataSet {
    private static final String SELECT = "SELECT c.*, "
            + "(SELECT p.nombre FROM Suscripciones s JOIN DetallesSuscripciones d ON d.suscripcionId=s.suscripcionId AND d.estado='Activo' "
            + "AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3) JOIN Planes p ON p.planId=d.planId WHERE s.clienteId=c.clienteId "
            + "ORDER BY d.fechaInicio DESC LIMIT 1) AS planNombre, "
            + "(SELECT d.fechaFin FROM Suscripciones s JOIN DetallesSuscripciones d ON d.suscripcionId=s.suscripcionId AND d.estado='Activo' "
            + "AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3) WHERE s.clienteId=c.clienteId ORDER BY d.fechaInicio DESC LIMIT 1) AS fechaFinPlan, "
            + "(SELECT p.peticionesMes FROM Suscripciones s JOIN DetallesSuscripciones d ON d.suscripcionId=s.suscripcionId AND d.estado='Activo' "
            + "AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3) JOIN Planes p ON p.planId=d.planId WHERE s.clienteId=c.clienteId "
            + "ORDER BY d.fechaInicio DESC LIMIT 1) AS peticionesMes, "
            + "(SELECT p.conexionesSimultaneas FROM Suscripciones s JOIN DetallesSuscripciones d ON d.suscripcionId=s.suscripcionId AND d.estado='Activo' "
            + "AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3) JOIN Planes p ON p.planId=d.planId WHERE s.clienteId=c.clienteId "
            + "ORDER BY d.fechaInicio DESC LIMIT 1) AS conexionesSimultaneas, "
            + "(SELECT COALESCE(SUM(x.peticiones),0) FROM MetricasClientesDiarias x WHERE x.clienteId=c.clienteId AND x.fecha>=COALESCE((SELECT DATE(d.fechaInicio) "
            + "FROM Suscripciones s JOIN DetallesSuscripciones d ON d.suscripcionId=s.suscripcionId AND d.estado='Activo' AND d.fechaInicio<=NOW(3) "
            + "AND d.fechaFin>NOW(3) WHERE s.clienteId=c.clienteId ORDER BY d.fechaInicio DESC LIMIT 1), CURRENT_DATE)) AS peticionesUsadas "
            + "FROM Clientes c ";

    private final JdbcTemplate jdbc;
    private final PermisoJerarquiaClienteDAO permisos;

    public ClienteDAO(JdbcTemplate jdbc, PermisoJerarquiaClienteDAO permisos) {
        this.jdbc = jdbc;
        this.permisos = permisos;
    }

    @Override
    public Cliente create(ClienteDatosParameters parametro, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO Clientes (clienteUuid, tipoCliente, documento, nombreComercial, razonSocial, correo, prefijoTelefono, telefono, "
                        + "direccion, diaCorte, usuarioCreacionUuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuid, parametro.getTipoCliente(), vacioANulo(parametro.getDocumento()), parametro.getNombreComercial().trim(),
                parametro.getRazonSocial(), parametro.getCorreo().trim(), parametro.getPrefijoTelefono(), parametro.getTelefono(),
                parametro.getDireccion(), parametro.getDiaCorte() == null ? 1 : parametro.getDiaCorte(), actor);
        return findByUuid(uuid);
    }

    @Override
    public Cliente update(ClienteActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE Clientes SET tipoCliente=?, documento=?, nombreComercial=?, razonSocial=?, correo=?, "
                        + "prefijoTelefono=?, telefono=?, direccion=?, diaCorte=COALESCE(?,diaCorte), estado=COALESCE(NULLIF(?,''),estado), "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                        + "WHERE clienteUuid=? AND estado<>'Eliminado'",
                parametro.getTipoCliente(), vacioANulo(parametro.getDocumento()), parametro.getNombreComercial().trim(), parametro.getRazonSocial(),
                parametro.getCorreo().trim(), parametro.getPrefijoTelefono(), parametro.getTelefono(), parametro.getDireccion(),
                parametro.getDiaCorte(), parametro.getEstado(), actor, parametro.getClienteUuid());
        return actualizados == 0 ? null : findByUuid(parametro.getClienteUuid());
    }

    @Override
    public Cliente findById(Long id) {
        return jdbc.query(SELECT + "WHERE c.clienteId=? AND c.estado<>'Eliminado'", new BeanPropertyRowMapper<>(Cliente.class), id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public Cliente findByUuid(String uuid) {
        return jdbc.query(SELECT + "WHERE c.clienteUuid=?", new BeanPropertyRowMapper<>(Cliente.class), uuid).stream().findFirst().orElse(null);
    }

    @Override
    public List<Cliente> findAll() {
        return jdbc.query(SELECT + "WHERE c.estado<>'Eliminado' ORDER BY c.nombreComercial", new BeanPropertyRowMapper<>(Cliente.class));
    }

    @Override
    public PaginationResponse<Cliente> findAllPages(Integer page, Integer size, String buscar, String estado) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = size == null || size < 1 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder("WHERE c.estado<>'Eliminado' ");
        List<Object> argumentos = new ArrayList<>();
        if (buscar != null && !buscar.isBlank()) {
            where.append("AND (c.nombreComercial LIKE ? OR c.razonSocial LIKE ? OR c.documento LIKE ? OR c.correo LIKE ?) ");
            String patron = "%" + buscar.trim() + "%";
            for (int i = 0; i < 4; i++) argumentos.add(patron);
        }
        if (estado != null && !estado.isBlank()) {
            where.append("AND c.estado=? ");
            argumentos.add(estado);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM Clientes c " + where, Long.class, argumentos.toArray());
        List<Object> argumentosPagina = new ArrayList<>(argumentos);
        argumentosPagina.add(tamano);
        argumentosPagina.add((pagina - 1) * tamano);
        List<Cliente> registros = jdbc.query(SELECT + where + "ORDER BY c.fechaCreacion DESC LIMIT ? OFFSET ?",
                new BeanPropertyRowMapper<>(Cliente.class), argumentosPagina.toArray());
        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return new PaginationResponse<>(pagina, tamano, total, totalPaginas, pagina > 1, pagina < totalPaginas, registros);
    }

    @Override
    public boolean existsDocumento(String documento, String excluirUuid) {
        if (documento == null || documento.isBlank()) return false;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Clientes WHERE documento=? AND estado<>'Eliminado' AND clienteUuid<>COALESCE(?,'')",
                Integer.class, documento.trim(), excluirUuid);
        return total != null && total > 0;
    }

    @Override
    public Long crearSuscripcion(Long clienteId, String actor) {
        jdbc.update("INSERT IGNORE INTO Suscripciones (suscripcionUuid, clienteId, clienteUuid, usuarioCreacionUuid) "
                + "SELECT UUID(), clienteId, clienteUuid, ? FROM Clientes WHERE clienteId=?", actor, clienteId);
        return jdbc.queryForObject("SELECT suscripcionId FROM Suscripciones WHERE clienteId=?", Long.class, clienteId);
    }

    @Override
    public Long crearRolPropietario(Long clienteId, String nombre, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO RolesClientes (rolClienteUuid, clienteId, clienteUuid, nombre, descripcion, color, protegido, usuarioCreacionUuid) "
                + "SELECT ?, clienteId, clienteUuid, ?, 'Acceso completo a la cuenta del cliente.', '#e3121b', TRUE, ? FROM Clientes WHERE clienteId=?",
                uuid, nombre, actor, clienteId);
        Long rolId = jdbc.queryForObject("SELECT rolClienteId FROM RolesClientes WHERE rolClienteUuid=?", Long.class, uuid);
        permisos.otorgarProtegidos();
        return rolId;
    }

    @Override
    public boolean delete(Long id, String actor) {
        return jdbc.update("UPDATE Clientes SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE clienteId=? AND estado<>'Eliminado'", actor, id) > 0;
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
