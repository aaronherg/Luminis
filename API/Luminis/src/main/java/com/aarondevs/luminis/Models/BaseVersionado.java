package com.aarondevs.luminis.Models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseVersionado {
    private String usuarioCreacionUuid;
    private Timestamp fechaCreacion;
    private Timestamp fechaModificacion;
    private String usuarioModificacionUuid;
    private Timestamp fechaVersion;
    private Long version;
    private String estado;
}
