package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;

@Data
public class ServicioWhatsappParameters {
    private Boolean habilitado;
    private String dispositivo;
    private String apiKey;
    private String telefonoPrueba;
}
