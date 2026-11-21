package com.aarondevs.luminis.Parameters.Mensajeria;

import java.util.List;
import lombok.Data;

@Data
public class ApiKeyAgregarParameters {
    private String nombre;
    private List<String> permisos;
}
