package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PlanActualizarParameters extends PlanAgregarParameters {
    private String planUuid;
    private String estado;
}
