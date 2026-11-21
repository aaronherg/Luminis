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
public class UsuarioClienteRol extends BaseVersionado {
    private Long usuarioClienteRolId;
    private String usuarioClienteRolUuid;
    private Long usuarioClienteId;
    private String usuarioClienteUuid;
    private Long rolClienteId;
    private String rolClienteUuid;
}
