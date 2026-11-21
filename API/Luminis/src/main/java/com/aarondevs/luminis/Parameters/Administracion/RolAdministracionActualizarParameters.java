package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RolAdministracionActualizarParameters extends RolAdministracionAgregarParameters {
    private String rolAdministracionUuid;
    private String estado;
}
