package com.aarondevs.luminis.Models.Administracion;

import com.aarondevs.luminis.Models.BaseVersionado;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Moneda extends BaseVersionado {
    private Long monedaId;
    private String monedaUuid;
    private String codigo;
    private String nombre;
    private String simbolo;
    private BigDecimal tasa;
}
