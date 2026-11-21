package com.aarondevs.luminis.Parameters.Mensajeria;

import lombok.Data;

@Data
public class MonitorDesconectarParameters {
    private Long clienteId;
    private String usuario;
    private String sesion;
}
