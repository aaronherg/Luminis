package com.aarondevs.luminis.Parameters.Administracion;

import java.util.List;
import lombok.Data;

@Data
public class RolAdministracionAgregarParameters {
    private String nombre;
    private String descripcion;
    private String color;
    private List<Long> accionesIds;
}
