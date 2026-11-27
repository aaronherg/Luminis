package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.DAO.Seguridad.PermisoJerarquiaDataSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public final class SincronizadorPermisos {
    private SincronizadorPermisos() {
    }

    public static void sincronizar(ResourceLoader recursos, String ubicacion, PermisoJerarquiaDataSet datos) throws Exception {
        Resource recurso = recursos.getResource(ubicacion);
        if (!recurso.exists()) throw new IllegalStateException("No se encontró el catálogo de permisos: " + ubicacion);
        JsonNode modulos;
        try (InputStream entrada = recurso.getInputStream()) {
            modulos = new ObjectMapper().readTree(entrada).path("modulos");
        }
        if (!modulos.isArray() || modulos.isEmpty()) throw new IllegalStateException("El catálogo " + ubicacion + " debe incluir una lista 'modulos'");
        List<String> clavesModulos = new ArrayList<>();
        List<String> clavesCategorias = new ArrayList<>();
        Map<String, List<String>> opciones = new LinkedHashMap<>();
        int ordenModulo = 0;
        for (JsonNode modulo : modulos) {
            String claveModulo = requerido(modulo, "clave");
            clavesModulos.add(claveModulo);
            Long moduloId = datos.sincronizarModulo(claveModulo, requerido(modulo, "nombre"), modulo.path("descripcion").asText(null),
                    modulo.path("icono").asText(null), modulo.path("orden").asInt(++ordenModulo));
            for (JsonNode categoria : modulo.path("categorias")) {
                String claveCategoria = requerido(categoria, "clave");
                clavesCategorias.add(claveCategoria);
                Long categoriaId = datos.sincronizarCategoria(moduloId, claveCategoria, requerido(categoria, "nombre"), categoria.path("icono").asText(null),
                        categoria.path("orden").asInt(0), categoria.path("sola").asBoolean(false));
                for (JsonNode opcion : categoria.path("opciones")) {
                    String claveOpcion = requerido(opcion, "clave");
                    Long opcionId = datos.sincronizarOpcion(categoriaId, claveOpcion, requerido(opcion, "nombre"), opcion.path("descripcion").asText(null),
                            opcion.path("icono").asText(null), opcion.path("orden").asInt(0));
                    List<String> acciones = new ArrayList<>();
                    int orden = 0;
                    for (JsonNode accion : opcion.path("acciones")) {
                        String claveAccion = requerido(accion, "clave");
                        acciones.add(claveAccion);
                        datos.sincronizarAccion(opcionId, claveAccion, requerido(accion, "nombre"), ++orden);
                    }
                    opciones.put(claveOpcion, acciones);
                }
            }
        }
        datos.depurar(clavesModulos, clavesCategorias, opciones);
    }

    private static String requerido(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        if (valor == null || valor.isBlank()) throw new IllegalStateException("El catálogo de permisos tiene un elemento sin '" + campo + "'");
        return valor;
    }
}
