package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;

@Data
public class ClienteDatosParameters {
    private String tipoCliente;
    private String documento;
    private String nombreComercial;
    private String razonSocial;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private String direccion;
    private Integer diaCorte;
}
