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
public class ModuloAdministracionAccion extends BaseVersionado {
    private Long moduloAdministracionAccionId;
    private String moduloAdministracionAccionUuid;
    private Long moduloAdministracionId;
    private String moduloAdministracionUuid;
    private String clave;
    private String nombre;
    private Integer orden;
}
