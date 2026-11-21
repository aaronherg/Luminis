package com.aarondevs.luminis.Parameters.Mensajeria;

import java.util.List;
import lombok.Data;

@Data
public class PresenciaBuscarParameters {
    private List<String> usuarios;
    private Integer limite;
}
