package com.aarondevs.luminis.Models.Seguridad;

import com.aarondevs.luminis.Models.BaseVersionado;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Usuario extends BaseVersionado {
    private Long usuarioId;
    private String usuarioUuid;
    private String usuario;
    @JsonIgnore
    private String contrasena;
    private String tipo;
    private Boolean debeCambiarContrasena;
    private java.sql.Timestamp fechaUltimoAcceso;
    private Boolean dosFactores;
}
