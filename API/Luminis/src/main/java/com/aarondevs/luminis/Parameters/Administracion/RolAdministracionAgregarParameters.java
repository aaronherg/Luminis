package com.aarondevs.luminis.Parameters.Administracion;

import java.util.List;
import lombok.Data;

@Data
public class RolAdministracionAgregarParameters {
    private String nombre;
    private String descripcion;
    private String color;
    private List<Long> modulosIds;
    private List<Long> categoriasIds;
    private List<Long> opcionesIds;
    private List<Long> accionesIds;
}
