package com.aarondevs.luminis.Models.Administracion;

import com.aarondevs.luminis.Models.BaseVersionado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RolAdministracion extends BaseVersionado {
    private Long rolAdministracionId;
    private String rolAdministracionUuid;
    private String nombre;
    private String descripcion;
    private String color;
    private Boolean protegido;
    private java.util.List<Long> modulosIds;
    private java.util.List<Long> categoriasIds;
    private java.util.List<Long> opcionesIds;
    private java.util.List<Long> accionesIds;
}
