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
public class UsuarioAdministracionRol extends BaseVersionado {
    private Long usuarioAdministracionRolId;
    private String usuarioAdministracionRolUuid;
    private Long usuarioAdministracionId;
    private String usuarioAdministracionUuid;
    private Long rolAdministracionId;
    private String rolAdministracionUuid;
}
