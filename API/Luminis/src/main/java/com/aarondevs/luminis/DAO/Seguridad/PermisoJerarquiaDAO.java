package com.aarondevs.luminis.DAO.Seguridad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class PermisoJerarquiaDAO implements PermisoJerarquiaDataSet {
    private final JdbcTemplate jdbc;
    private final String area;
    private final String s;

    protected PermisoJerarquiaDAO(JdbcTemplate jdbc, String area, String singular) {
        this.jdbc = jdbc;
        this.area = area;
        this.s = singular;
    }

    private String modulos() { return "Modulos" + area; }
    private String categorias() { return "Categorias" + area; }
    private String opciones() { return "Opciones" + area; }
    private String acciones() { return "Opciones" + area + "Acciones"; }
    private String roles() { return "Roles" + area; }
    private String rolesTabla(String sufijo) { return "Roles" + area + sufijo; }
    private String usuariosRoles() { return "Usuarios" + area + "Roles"; }
    private String id(String nombre) { return nombre + s + "Id"; }
    private String uuid(String nombre) { return nombre + s + "Uuid"; }
    private String rol() { return "rol" + s + "Id"; }
    private String accionId() { return "opcion" + s + "AccionId"; }

    @Override
    public Long sincronizarModulo(String clave, String nombre, String descripcion, String icono, int orden) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("nombre", nombre);
        campos.put("descripcion", descripcion);
        campos.put("icono", icono);
        campos.put("orden", orden);
        return upsert(modulos(), "modulo", clave, campos);
    }

    @Override
    public Long sincronizarCategoria(Long moduloId, String clave, String nombre, String icono, int orden, boolean sola) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put(id("modulo"), moduloId);
        campos.put("nombre", nombre);
        campos.put("icono", icono);
        campos.put("orden", orden);
        campos.put("sola", sola);
        return upsert(categorias(), "categoria", clave, campos);
    }

    @Override
    public Long sincronizarOpcion(Long categoriaId, String clave, String nombre, String descripcion, String icono, int orden) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put(id("categoria"), categoriaId);
        campos.put("nombre", nombre);
        campos.put("descripcion", descripcion);
        campos.put("icono", icono);
        campos.put("orden", orden);
        return upsert(opciones(), "opcion", clave, campos);
    }

    private Long upsert(String tabla, String entidad, String clave, Map<String, Object> campos) {
        String pk = id(entidad);
        Long actual = jdbc.query("SELECT " + pk + " FROM " + tabla + " WHERE clave=?", (rs, i) -> rs.getLong(1), clave).stream().findFirst().orElse(null);
        List<Object> valores = new ArrayList<>(campos.values());
        if (actual == null) {
            String columnas = String.join(", ", campos.keySet());
            String marcas = String.join(", ", Collections.nCopies(campos.size(), "?"));
            valores.add(0, clave);
            jdbc.update("INSERT INTO " + tabla + " (" + uuid(entidad) + ", clave, " + columnas + ", usuarioCreacionUuid) VALUES (UUID(), ?, " + marcas + ", 'SISTEMA')", valores.toArray());
            return jdbc.queryForObject("SELECT " + pk + " FROM " + tabla + " WHERE clave=?", Long.class, clave);
        }
        String asignaciones = String.join(", ", campos.keySet().stream().map(c -> c + "=?").toList());
        valores.add(actual);
        jdbc.update("UPDATE " + tabla + " SET " + asignaciones + ", estado='Activo' WHERE " + pk + "=?", valores.toArray());
        return actual;
    }

    @Override
    public void sincronizarAccion(Long opcionId, String clave, String nombre, int orden) {
        int actualizadas = jdbc.update("UPDATE " + acciones() + " SET nombre=?, orden=?, estado='Activo' WHERE " + id("opcion") + "=? AND clave=?", nombre, orden, opcionId, clave);
        if (actualizadas == 0) {
            jdbc.update("INSERT INTO " + acciones() + " (opcion" + s + "AccionUuid, " + id("opcion") + ", " + uuid("opcion") + ", clave, nombre, orden, usuarioCreacionUuid) "
                    + "SELECT UUID(), " + id("opcion") + ", " + uuid("opcion") + ", ?, ?, ?, 'SISTEMA' FROM " + opciones() + " WHERE " + id("opcion") + "=?", clave, nombre, orden, opcionId);
        }
    }

    @Override
    public void depurar(Collection<String> claveModulos, Collection<String> claveCategorias, Map<String, ? extends Collection<String>> claveOpciones) {
        eliminarAusentes(modulos(), claveModulos);
        eliminarAusentes(categorias(), claveCategorias);
        eliminarAusentes(opciones(), claveOpciones.keySet());
        for (Map<String, Object> a : jdbc.queryForList("SELECT a." + accionId() + " AS id, o.clave AS opcion, a.clave AS accion FROM " + acciones() + " a JOIN " + opciones()
                + " o ON o." + id("opcion") + "=a." + id("opcion") + " WHERE a.estado='Activo'")) {
            Collection<String> vigentes = claveOpciones.get(String.valueOf(a.get("opcion")));
            if (vigentes == null || !vigentes.contains(String.valueOf(a.get("accion")))) {
                jdbc.update("UPDATE " + acciones() + " SET estado='Eliminado' WHERE " + accionId() + "=?", a.get("id"));
            }
        }
    }

    private void eliminarAusentes(String tabla, Collection<String> claves) {
        if (claves.isEmpty()) {
            jdbc.update("UPDATE " + tabla + " SET estado='Eliminado' WHERE estado='Activo'");
            return;
        }
        String marcas = String.join(",", Collections.nCopies(claves.size(), "?"));
        jdbc.update("UPDATE " + tabla + " SET estado='Eliminado' WHERE estado='Activo' AND clave NOT IN (" + marcas + ")", claves.toArray());
    }

    @Override
    public void otorgarProtegidos() {
        otorgarTodo("Modulos", "Modulo", modulos(), id("modulo"));
        otorgarTodo("Categorias", "Categoria", categorias(), id("categoria"));
        otorgarTodo("Opciones", "Opcion", opciones(), id("opcion"));
        otorgarTodo("Acciones", "Accion", acciones(), accionId());
    }

    private void otorgarTodo(String sufijo, String entidad, String catalogo, String columna) {
        String tabla = rolesTabla(sufijo);
        jdbc.update("INSERT INTO " + tabla + " (rol" + s + entidad + "Uuid, " + rol() + ", " + columna + ", usuarioCreacionUuid) SELECT UUID(), r." + rol() + ", c." + columna + ", 'SISTEMA' FROM "
                + roles() + " r CROSS JOIN " + catalogo + " c WHERE r.protegido=TRUE AND r.estado<>'Eliminado' AND c.estado='Activo' "
                + "ON DUPLICATE KEY UPDATE " + tabla + ".estado='Activo'");
    }

    @Override
    public void otorgarVer() {
        String tabla = rolesTabla("Acciones");
        jdbc.update("INSERT INTO " + tabla + " (rol" + s + "AccionUuid, " + rol() + ", " + accionId() + ", usuarioCreacionUuid) SELECT UUID(), ro." + rol() + ", a." + accionId() + ", 'SISTEMA' FROM "
                + rolesTabla("Opciones") + " ro JOIN " + acciones() + " a ON a." + id("opcion") + "=ro." + id("opcion") + " AND a.clave='ver' AND a.estado='Activo' WHERE ro.estado='Activo' "
                + "ON DUPLICATE KEY UPDATE " + tabla + ".estado='Activo'");
    }

    @Override
    public List<Map<String, Object>> arbol() {
        List<Map<String, Object>> mods = jdbc.queryForList("SELECT " + id("modulo") + " AS id, clave, nombre, descripcion, icono FROM " + modulos()
                + " WHERE estado='Activo' ORDER BY orden, " + id("modulo"));
        List<Map<String, Object>> cats = jdbc.queryForList("SELECT " + id("categoria") + " AS id, " + id("modulo") + " AS padre, clave, nombre, icono, sola FROM " + categorias()
                + " WHERE estado='Activo' ORDER BY orden, " + id("categoria"));
        List<Map<String, Object>> ops = jdbc.queryForList("SELECT " + id("opcion") + " AS id, " + id("categoria") + " AS padre, clave, nombre, descripcion, icono FROM " + opciones()
                + " WHERE estado='Activo' ORDER BY orden, " + id("opcion"));
        List<Map<String, Object>> accs = jdbc.queryForList("SELECT " + accionId() + " AS id, " + id("opcion") + " AS padre, clave, nombre FROM " + acciones()
                + " WHERE estado='Activo' ORDER BY orden, " + accionId());
        colgar(ops, accs, "acciones");
        colgar(cats, ops, "opciones");
        colgar(mods, cats, "categorias");
        cats.forEach(c -> c.put("sola", verdadero(c.get("sola"))));
        return mods;
    }

    private void colgar(List<Map<String, Object>> padres, List<Map<String, Object>> hijos, String campo) {
        Map<Long, List<Map<String, Object>>> grupos = new LinkedHashMap<>();
        for (Map<String, Object> h : hijos) {
            Object padre = h.remove("padre");
            if (padre != null) grupos.computeIfAbsent(((Number) padre).longValue(), k -> new ArrayList<>()).add(h);
        }
        for (Map<String, Object> padre : padres) padre.put(campo, grupos.getOrDefault(((Number) padre.get("id")).longValue(), new ArrayList<>()));
    }

    @Override
    public Map<String, List<Long>> permisosRol(Long rolId) {
        Map<String, List<Long>> r = new LinkedHashMap<>();
        r.put("modulosIds", ids("Modulos", modulos(), id("modulo"), rolId));
        r.put("categoriasIds", ids("Categorias", categorias(), id("categoria"), rolId));
        r.put("opcionesIds", ids("Opciones", opciones(), id("opcion"), rolId));
        r.put("accionesIds", ids("Acciones", acciones(), accionId(), rolId));
        return r;
    }

    private List<Long> ids(String sufijo, String catalogo, String columna, Long rolId) {
        return jdbc.queryForList("SELECT x." + columna + " FROM " + rolesTabla(sufijo) + " x JOIN " + catalogo + " c ON c." + columna + "=x." + columna
                + " AND c.estado='Activo' WHERE x." + rol() + "=? AND x.estado='Activo'", Long.class, rolId);
    }

    @Override
    public void guardarPermisos(Long rolId, List<Long> modulosIds, List<Long> categoriasIds, List<Long> opcionesIds, List<Long> accionesIds, String actor) {
        Set<Long> m = conjunto(modulosIds), c = conjunto(categoriasIds), o = conjunto(opcionesIds), a = conjunto(accionesIds);
        List<Long> mods = new ArrayList<>(), cats = new ArrayList<>(), ops = new ArrayList<>(), accs = new ArrayList<>();
        for (Map<String, Object> modulo : arbol()) {
            if (!m.contains(numero(modulo.get("id")))) continue;
            boolean conCategoria = false;
            for (Map<String, Object> categoria : hijos(modulo, "categorias")) {
                if (!c.contains(numero(categoria.get("id")))) continue;
                boolean conOpcion = false;
                for (Map<String, Object> opcion : hijos(categoria, "opciones")) {
                    if (!o.contains(numero(opcion.get("id")))) continue;
                    conOpcion = true;
                    ops.add(numero(opcion.get("id")));
                    for (Map<String, Object> accion : hijos(opcion, "acciones")) {
                        Long accionId = numero(accion.get("id"));
                        if ("ver".equals(accion.get("clave")) || a.contains(accionId)) accs.add(accionId);
                    }
                }
                if (conOpcion) {
                    conCategoria = true;
                    cats.add(numero(categoria.get("id")));
                }
            }
            if (conCategoria) mods.add(numero(modulo.get("id")));
        }
        reemplazar("Modulos", "Modulo", id("modulo"), rolId, mods, actor);
        reemplazar("Categorias", "Categoria", id("categoria"), rolId, cats, actor);
        reemplazar("Opciones", "Opcion", id("opcion"), rolId, ops, actor);
        reemplazar("Acciones", "Accion", accionId(), rolId, accs, actor);
    }

    @Override
    public void revocarPermisos(Long rolId, String actor) {
        for (String sufijo : List.of("Modulos", "Categorias", "Opciones", "Acciones")) {
            jdbc.update("UPDATE " + rolesTabla(sufijo) + " SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? WHERE "
                    + rol() + "=? AND estado<>'Eliminado'", actor, rolId);
        }
    }

    private void reemplazar(String sufijo, String entidad, String columna, Long rolId, List<Long> valores, String actor) {
        String tabla = rolesTabla(sufijo);
        jdbc.update("UPDATE " + tabla + " SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? WHERE "
                + rol() + "=? AND estado<>'Eliminado'", actor, rolId);
        for (Long valor : valores) {
            jdbc.update("INSERT INTO " + tabla + " (rol" + s + entidad + "Uuid, " + rol() + ", " + columna + ", usuarioCreacionUuid) VALUES (UUID(), ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE " + tabla + ".estado='Activo', " + tabla + ".fechaModificacion=NOW(3), " + tabla + ".fechaVersion=NOW(3), "
                    + tabla + ".version=" + tabla + ".version+1, " + tabla + ".usuarioModificacionUuid=VALUES(usuarioCreacionUuid)", rolId, valor, actor);
        }
    }

    private String sqlOpcionesEfectivas() {
        String r = rol();
        return "FROM " + usuariosRoles() + " ur "
                + "JOIN " + roles() + " r ON r." + r + "=ur." + r + " AND r.estado='Activo' "
                + "JOIN " + rolesTabla("Modulos") + " rm ON rm." + r + "=r." + r + " AND rm.estado='Activo' "
                + "JOIN " + modulos() + " mo ON mo." + id("modulo") + "=rm." + id("modulo") + " AND mo.estado='Activo' "
                + "JOIN " + rolesTabla("Categorias") + " rc ON rc." + r + "=r." + r + " AND rc.estado='Activo' "
                + "JOIN " + categorias() + " ca ON ca." + id("categoria") + "=rc." + id("categoria") + " AND ca." + id("modulo") + "=mo." + id("modulo") + " AND ca.estado='Activo' "
                + "JOIN " + rolesTabla("Opciones") + " ro ON ro." + r + "=r." + r + " AND ro.estado='Activo' "
                + "JOIN " + opciones() + " op ON op." + id("opcion") + "=ro." + id("opcion") + " AND op." + id("categoria") + "=ca." + id("categoria") + " AND op.estado='Activo' ";
    }

    @Override
    public Set<String> acciones(Long usuarioId) {
        if (usuarioId == null) return Set.of();
        return new LinkedHashSet<>(jdbc.queryForList("SELECT DISTINCT CONCAT(op.clave, '.', ac.clave) " + sqlOpcionesEfectivas()
                + "JOIN " + rolesTabla("Acciones") + " ra ON ra." + rol() + "=r." + rol() + " AND ra.estado='Activo' "
                + "JOIN " + acciones() + " ac ON ac." + accionId() + "=ra." + accionId() + " AND ac." + id("opcion") + "=op." + id("opcion") + " AND ac.estado='Activo' "
                + "WHERE ur." + id("usuario") + "=? AND ur.estado='Activo'", String.class, usuarioId));
    }

    @Override
    public List<Map<String, Object>> menu(Long usuarioId) {
        if (usuarioId == null) return List.of();
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT op.clave, op.nombre, op.descripcion, op.icono, op.orden, ca.clave AS categoria, "
                + "CASE WHEN ca.sola THEN NULL ELSE ca.nombre END AS categoriaNombre, ca.icono AS categoriaIcono, ca.orden AS categoriaOrden, mo.clave AS modulo, "
                + "MIN(mo.orden) AS moduloOrden " + sqlOpcionesEfectivas() + "WHERE ur." + id("usuario") + "=? AND ur.estado='Activo' "
                + "GROUP BY op." + id("opcion") + ", op.clave, op.nombre, op.descripcion, op.icono, op.orden, ca.clave, ca.sola, ca.nombre, ca.icono, ca.orden, mo.clave "
                + "ORDER BY moduloOrden, ca.orden, op.orden", usuarioId);
        filas.forEach(f -> f.remove("moduloOrden"));
        return filas;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> hijos(Map<String, Object> nodo, String campo) {
        Object v = nodo.get(campo);
        return v instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private Set<Long> conjunto(List<Long> valores) {
        return valores == null ? Set.of() : new HashSet<>(valores);
    }

    private static Long numero(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }

    private static boolean verdadero(Object v) {
        return v instanceof Boolean b ? b : v instanceof Number n && n.intValue() != 0;
    }
}
