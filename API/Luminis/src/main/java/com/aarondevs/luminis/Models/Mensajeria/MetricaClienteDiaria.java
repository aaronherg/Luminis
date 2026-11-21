package com.aarondevs.luminis.Models.Mensajeria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricaClienteDiaria {
    private Long metricaClienteDiariaId;
    private String metricaClienteDiariaUuid;
    private Long clienteId;
    private String clienteUuid;
    private java.sql.Date fecha;
    private Long peticiones;
    private Long mensajes;
    private Long entregados;
    private Long vistos;
    private Long desaparecidos;
    private Long rechazados;
    private Long solicitudesApi;
    private Long conexiones;
    private Integer picoConexiones;
    private Long bytes;
}
