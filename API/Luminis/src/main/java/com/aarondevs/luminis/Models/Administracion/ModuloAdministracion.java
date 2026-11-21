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
public class ModuloAdministracion extends BaseVersionado {
    private Long moduloAdministracionId;
    private String moduloAdministracionUuid;
    private String clave;
    private String nombre;
    private String descripcion;
    private String icono;
    private Integer orden;
    private String categoria;
    private String categoriaNombre;
    private String categoriaIcono;
    private Integer categoriaOrden;
    private java.util.List<ModuloAdministracionAccion> acciones;
}
