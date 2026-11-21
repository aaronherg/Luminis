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
public class ModuloCliente extends BaseVersionado {
    private Long moduloClienteId;
    private String moduloClienteUuid;
    private String clave;
    private String nombre;
    private String descripcion;
    private String icono;
    private Integer orden;
    private String categoria;
    private String categoriaNombre;
    private String categoriaIcono;
    private Integer categoriaOrden;
    private java.util.List<ModuloClienteAccion> acciones;
}
