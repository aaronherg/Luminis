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
public class UsuarioAdministracion extends BaseVersionado {
    private Long usuarioAdministracionId;
    private String usuarioAdministracionUuid;
    private Long usuarioId;
    private String usuarioUuid;
    private String nombres;
    private String apellidos;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private String documento;
    private String direccion;
    private String whatsapp;
    private String usuario;
    private java.util.List<Long> rolesIds;
    private java.util.List<String> rolesNombres;
}
