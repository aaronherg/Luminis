package com.aarondevs.luminis.DAO.Mensajeria;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MetricaDAO implements MetricaDataSet {
    public static final List<String> CAMPOS = List.of("peticiones", "mensajes", "entregados", "vistos", "desaparecidos", "rechazados",
            "solicitudesApi", "conexiones", "bytes");

    private final JdbcTemplate jdbc;

    public MetricaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acumular(Long clienteId, LocalDate fecha, Map<String, Long> valores, int picoConexiones) {
        String columnas = String.join(", ", CAMPOS);
        String marcadores = CAMPOS.stream().map(c -> "?").collect(Collectors.joining(", "));
        String actualizar = CAMPOS.stream().map(c -> c + "=" + c + "+VALUES(" + c + ")").collect(Collectors.joining(", "));
        List<Object> argumentos = new ArrayList<>();
        argumentos.add(UUID.randomUUID().toString());
        argumentos.add(Date.valueOf(fecha));
        for (String campo : CAMPOS) argumentos.add(valores.getOrDefault(campo, 0L));
        argumentos.add(picoConexiones);
        argumentos.add(clienteId);
        jdbc.update("INSERT INTO MetricasClientesDiarias (metricaClienteDiariaUuid, clienteId, clienteUuid, fecha, " + columnas + ", picoConexiones) "
                + "SELECT ?, c.clienteId, c.clienteUuid, ?, " + marcadores + ", ? FROM Clientes c WHERE c.clienteId=? "
                + "ON DUPLICATE KEY UPDATE " + actualizar + ", picoConexiones=GREATEST(picoConexiones, VALUES(picoConexiones))", argumentos.toArray());
    }

    @Override
    public Set<Long> apiKeysVigentes(Collection<Long> apiKeyIds) {
        if (apiKeyIds.isEmpty()) return Set.of();
        String marcadores = apiKeyIds.stream().map(id -> "?").collect(Collectors.joining(","));
        return new HashSet<>(jdbc.query("SELECT k.apiKeyId FROM ApiKeys k JOIN Clientes c ON c.clienteId=k.clienteId "
                + "WHERE k.estado='Activo' AND c.estado='Activo' AND k.apiKeyId IN (" + marcadores + ")", (rs, i) -> rs.getLong(1), apiKeyIds.toArray()));
    }
}
