package com.aarondevs.luminis.Parameters.Administracion;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MonedaAgregarParameters {
    private String codigo;
    private String nombre;
    private String simbolo;
    private BigDecimal tasa;
}
