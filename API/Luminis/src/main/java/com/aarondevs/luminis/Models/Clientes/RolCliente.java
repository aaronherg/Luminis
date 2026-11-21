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
public class RolCliente extends BaseVersionado {
    private Long rolClienteId;
    private String rolClienteUuid;
    private Long clienteId;
    private String clienteUuid;
    private String nombre;
    private String descripcion;
    private String color;
    private Boolean protegido;
    private java.util.List<Long> accionesIds;
}
