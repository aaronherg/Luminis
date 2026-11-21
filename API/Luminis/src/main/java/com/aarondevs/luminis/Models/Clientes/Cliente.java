package com.aarondevs.luminis.Models.Clientes;

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
public class Cliente extends BaseVersionado {
    private Long clienteId;
    private String clienteUuid;
    private String tipoCliente;
    private String documento;
    private String nombreComercial;
    private String razonSocial;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private String direccion;
    private Integer diaCorte;
    private String planNombre;
    private java.sql.Timestamp fechaFinPlan;
    private Long peticionesMes;
    private Integer conexionesSimultaneas;
    private Long peticionesUsadas;
}
