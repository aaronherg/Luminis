package com.aarondevs.luminis.Parameters.Clientes;

import java.util.List;
import lombok.Data;

@Data
public class RolClienteAgregarParameters {
    private Long clienteId;
    private String nombre;
    private String descripcion;
    private String color;
    private List<Long> accionesIds;
}
