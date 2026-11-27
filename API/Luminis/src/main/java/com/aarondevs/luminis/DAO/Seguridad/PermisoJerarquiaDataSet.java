package com.aarondevs.luminis.DAO.Seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermisoJerarquiaDataSet {
    Long sincronizarModulo(String clave, String nombre, String descripcion, String icono, int orden);
    Long sincronizarCategoria(Long moduloId, String clave, String nombre, String icono, int orden, boolean sola);
    Long sincronizarOpcion(Long categoriaId, String clave, String nombre, String descripcion, String icono, int orden);
    void sincronizarAccion(Long opcionId, String clave, String nombre, int orden);
    void depurar(Collection<String> modulos, Collection<String> categorias, Map<String, ? extends Collection<String>> opciones);
    void otorgarProtegidos();
    void otorgarVer();
    List<Map<String, Object>> arbol();
    Map<String, List<Long>> permisosRol(Long rolId);
    void guardarPermisos(Long rolId, List<Long> modulos, List<Long> categorias, List<Long> opciones, List<Long> acciones, String actor);
    void revocarPermisos(Long rolId, String actor);
    Set<String> acciones(Long usuarioId);
    List<Map<String, Object>> menu(Long usuarioId);
}
