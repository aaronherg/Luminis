package com.aarondevs.luminis.Models.Clientes;

import com.aarondevs.luminis.Models.BaseVersionado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModuloClienteAccion extends BaseVersionado {
    private Long moduloClienteAccionId;
    private String moduloClienteAccionUuid;
    private Long moduloClienteId;
    private String moduloClienteUuid;
    private String clave;
    private String nombre;
    private Integer orden;
}
