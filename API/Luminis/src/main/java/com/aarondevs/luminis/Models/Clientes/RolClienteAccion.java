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
public class RolClienteAccion extends BaseVersionado {
    private Long rolClienteAccionId;
    private String rolClienteAccionUuid;
    private Long rolClienteId;
    private String rolClienteUuid;
    private Long moduloClienteAccionId;
    private String moduloClienteAccionUuid;
}
