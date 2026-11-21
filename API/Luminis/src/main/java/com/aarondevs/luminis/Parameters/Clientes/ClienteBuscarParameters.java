package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;

@Data
public class ClienteBuscarParameters {
    private Integer page;
    private Integer size;
    private String buscar;
    private String estado;
}
