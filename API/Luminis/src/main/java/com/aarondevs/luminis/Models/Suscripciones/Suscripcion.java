package com.aarondevs.luminis.Models.Suscripciones;

import com.aarondevs.luminis.Models.BaseVersionado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Suscripcion extends BaseVersionado {
    private Long suscripcionId;
    private String suscripcionUuid;
    private Long clienteId;
    private String clienteUuid;
    private String clienteNombre;
    private DetalleSuscripcion vigente;
}
