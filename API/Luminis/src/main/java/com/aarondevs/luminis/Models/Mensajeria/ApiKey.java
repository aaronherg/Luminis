package com.aarondevs.luminis.Models.Mensajeria;

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
public class ApiKey extends BaseVersionado {
    private Long apiKeyId;
    private String apiKeyUuid;
    private Long clienteId;
    private String clienteUuid;
    private String nombre;
    private String prefijo;
    @JsonIgnore
    private String hash;
    private String permisos;
    private java.sql.Timestamp fechaUltimoUso;
    private String clave;
}
