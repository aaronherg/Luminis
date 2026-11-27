const ICONOS = {
    modulo: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
    chevron: '<path d="m7 10 5 5 5-5"/>',
    check: '<path d="m5 12 5 5 9-10"/>',
    escudo: '<path d="M12 3 5 6v6c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9V6z"/><path d="m9 12 2 2 4-4"/>'
};
const svg = (p) => `<svg viewBox="0 0 24 24" aria-hidden="true">${p}</svg>`;
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;
const extras = (o) => (o.acciones || []).filter((a) => a.clave !== "ver");

export function editorPermisos(contenedor, { arbol = [], rol = {}, protegido = false, icono = null } = {}) {
    const modulos = Array.isArray(arbol) ? arbol : [];
    const todas = { m: [], c: [], o: [], a: [] };
    for (const m of modulos) {
        todas.m.push(m.id);
        for (const c of m.categorias || []) {
            todas.c.push(c.id);
            for (const o of c.opciones || []) {
                todas.o.push(o.id);
                for (const a of o.acciones || []) todas.a.push(a.id);
            }
        }
    }
    const sel = protegido
        ? { m: new Set(todas.m), o: new Set(todas.o), a: new Set(todas.a) }
        : { m: new Set(rol.modulosIds || []), o: new Set(rol.opcionesIds || []), a: new Set(rol.accionesIds || []) };
    let abierta = null;
    const iconoModulo = (m) => {
        const propio = icono && m.icono ? icono(m.icono) : "";
        return propio && !/<svg[^>]*>\s*<\/svg>/.test(propio) ? propio : svg(ICONOS.modulo);
    };

    function marcarOpcion(o, activo, conAcciones = false) {
        if (activo) {
            sel.o.add(o.id);
            if (conAcciones) (o.acciones || []).forEach((a) => sel.a.add(a.id));
        } else {
            sel.o.delete(o.id);
            (o.acciones || []).forEach((a) => sel.a.delete(a.id));
        }
    }

    function marcarModulo(m, activo) {
        if (activo) { sel.m.add(m.id); return; }
        sel.m.delete(m.id);
        (m.categorias || []).forEach((c) => (c.opciones || []).forEach((o) => marcarOpcion(o, false)));
    }

    const elegidas = (c) => (c.opciones || []).filter((o) => sel.o.has(o.id)).length;

    function columnaModulos() {
        if (!modulos.length) return '<div class="rp-vacio">No hay módulos disponibles.</div>';
        return modulos.map((m) => {
            const activo = sel.m.has(m.id);
            const categorias = (m.categorias || []).length;
            return `<label class="rp-item rp-modulo${activo ? " is-on" : ""}"><input type="checkbox" data-rp-modulo="${m.id}"${activo ? " checked" : ""}${protegido ? " disabled" : ""}>
                <span class="rp-tile">${iconoModulo(m)}</span><span class="rp-texto"><strong>${esc(m.nombre)}</strong><small>${plural(categorias, "categoría disponible", "categorías disponibles")}</small></span></label>`;
        }).join("");
    }

    function columnaCategorias() {
        const activos = modulos.filter((m) => sel.m.has(m.id));
        if (!activos.length) return '<div class="rp-vacio">Selecciona un módulo para continuar.</div>';
        return activos.map((m) => `${activos.length > 1 ? `<p class="rp-separador">${esc(m.nombre)}</p>` : ""}${(m.categorias || []).map((c) => {
            const n = elegidas(c);
            const total = (c.opciones || []).length;
            const abierto = abierta === c.id;
            return `<section class="rp-cat${abierto ? " is-open" : ""}${n ? " is-on" : ""}"><div class="rp-cat-cab">
                <button type="button" class="rp-cat-abrir" data-rp-abrir="${c.id}" aria-expanded="${abierto}"><span class="rp-chevron">${svg(ICONOS.chevron)}</span><span class="rp-texto"><strong>${esc(c.nombre)}</strong><small>${n} de ${total} seleccionadas</small></span></button>
                ${protegido ? "" : `<button type="button" class="rp-mini" data-rp-categoria="${c.id}">${n === total && total ? "Desmarcar" : "Marcar todas"}</button>`}</div>
                ${abierto ? `<div class="rp-cat-opciones">${(c.opciones || []).map((o) => {
                    const on = sel.o.has(o.id);
                    const k = extras(o).length;
                    return `<label class="rp-item${on ? " is-on" : ""}"><input type="checkbox" data-rp-opcion="${o.id}"${on ? " checked" : ""}${protegido ? " disabled" : ""}><span class="rp-texto"><strong>${esc(o.nombre)}</strong><small>${k ? plural(k, "acción", "acciones") : "Acceso directo"}</small></span></label>`;
                }).join("")}</div>` : ""}</section>`;
        }).join("")}`).join("");
    }

    function columnaAcciones() {
        const grupos = [];
        let directas = 0;
        for (const m of modulos) {
            if (!sel.m.has(m.id)) continue;
            for (const c of m.categorias || []) for (const o of c.opciones || []) {
                if (!sel.o.has(o.id)) continue;
                if (extras(o).length) grupos.push({ c, o }); else directas += 1;
            }
        }
        const info = (titulo, texto, compacta = false) => `<div class="rp-info${compacta ? " is-compacta" : ""}"><span>${svg(ICONOS.check)}</span><div><strong>${titulo}</strong><p>${texto}</p></div></div>`;
        if (!grupos.length) return directas ? info("Acceso configurado", "Las opciones seleccionadas no necesitan acciones adicionales.") : '<div class="rp-vacio">Selecciona al menos una opción.</div>';
        return (directas ? info(plural(directas, "acceso directo", "accesos directos"), "No requieren permisos adicionales.", true) : "") + grupos.map(({ c, o }) => {
            const lista = extras(o);
            const todasOn = lista.every((a) => sel.a.has(a.id));
            return `<section class="rp-grupo"><div class="rp-grupo-cab"><div><strong>${esc(o.nombre)}</strong><small>${esc(c.nombre)}</small></div>${protegido ? "" : `<button type="button" class="rp-mini" data-rp-todas="${o.id}">${todasOn ? "Ninguna" : "Todas"}</button>`}</div>
                <div class="rp-acciones">${lista.map((a) => {
                    const on = sel.a.has(a.id);
                    return `<label class="rp-accion${on ? " is-on" : ""}"><input type="checkbox" data-rp-accion="${a.id}"${on ? " checked" : ""}${protegido ? " disabled" : ""}><span class="rp-marca">${svg(ICONOS.check)}</span><span>${esc(a.nombre)}</span></label>`;
                }).join("")}</div></section>`;
        }).join("");
    }

    function resumen() {
        const v = valor();
        if (!v.opcionesIds.length) return "Sin permisos seleccionados";
        return `${plural(v.modulosIds.length, "módulo", "módulos")} · ${plural(v.categoriasIds.length, "categoría", "categorías")} · ${plural(v.opcionesIds.length, "opción", "opciones")} · ${plural(v.accionesIds.length - v.opcionesIds.length, "acción", "acciones")}`;
    }

    function pintar() {
        const scroll = [...contenedor.querySelectorAll("[data-rp-lista]")].map((l) => l.scrollTop);
        contenedor.innerHTML = `<section class="rp${protegido ? " is-protegido" : ""}"><header class="rp-cab"><div><h3>Permisos del rol</h3><small>${protegido ? "El rol protegido siempre conserva todos los permisos." : "Elige los módulos, luego las categorías y opciones del menú y por último las acciones dentro de cada pantalla."}</small></div><span class="rp-resumen" data-rp-resumen>${esc(resumen())}</span></header>
            ${protegido ? `<div class="rp-protegido">${svg(ICONOS.escudo)}<span>Este rol tiene acceso completo y se actualiza solo cuando se agregan opciones nuevas.</span></div>` : ""}
            <div class="rp-columnas">
                <section class="rp-col"><div class="rp-col-cab"><span class="rp-paso">1</span><div><h4>Módulos</h4><p>Selecciona los módulos permitidos.</p></div></div><div class="rp-lista" data-rp-lista>${columnaModulos()}</div></section>
                <section class="rp-col"><div class="rp-col-cab"><span class="rp-paso">2</span><div><h4>Categorías y opciones</h4><p>Define lo que verá en el menú.</p></div></div><div class="rp-lista" data-rp-lista>${columnaCategorias()}</div></section>
                <section class="rp-col"><div class="rp-col-cab"><span class="rp-paso">3</span><div><h4>Acciones</h4><p>Indica qué podrá hacer en cada pantalla.</p></div></div><div class="rp-lista" data-rp-lista>${columnaAcciones()}</div></section>
            </div></section>`;
        contenedor.querySelectorAll("[data-rp-lista]").forEach((l, i) => { l.scrollTop = scroll[i] || 0; });
    }

    const buscar = (id, tipo) => {
        for (const m of modulos) {
            if (tipo === "m" && m.id === id) return m;
            for (const c of m.categorias || []) {
                if (tipo === "c" && c.id === id) return c;
                for (const o of c.opciones || []) if (tipo === "o" && o.id === id) return o;
            }
        }
        return null;
    };

    contenedor.addEventListener("change", (e) => {
        if (protegido) return;
        const t = e.target;
        if (t.dataset.rpModulo) { const m = buscar(Number(t.dataset.rpModulo), "m"); if (m) marcarModulo(m, t.checked); }
        else if (t.dataset.rpOpcion) { const o = buscar(Number(t.dataset.rpOpcion), "o"); if (o) marcarOpcion(o, t.checked); }
        else if (t.dataset.rpAccion) { const id = Number(t.dataset.rpAccion); if (t.checked) sel.a.add(id); else sel.a.delete(id); }
        else return;
        pintar();
    });

    contenedor.addEventListener("click", (e) => {
        const abrir = e.target.closest("[data-rp-abrir]");
        if (abrir) { const id = Number(abrir.dataset.rpAbrir); abierta = abierta === id ? null : id; pintar(); return; }
        if (protegido) return;
        const cat = e.target.closest("[data-rp-categoria]");
        if (cat) {
            const c = buscar(Number(cat.dataset.rpCategoria), "c");
            if (!c) return;
            const marcar = (c.opciones || []).some((o) => !sel.o.has(o.id));
            (c.opciones || []).forEach((o) => marcarOpcion(o, marcar, true));
            abierta = c.id;
            pintar();
            return;
        }
        const todasBtn = e.target.closest("[data-rp-todas]");
        if (todasBtn) {
            const o = buscar(Number(todasBtn.dataset.rpTodas), "o");
            if (!o) return;
            const lista = extras(o);
            const marcar = lista.some((a) => !sel.a.has(a.id));
            lista.forEach((a) => (marcar ? sel.a.add(a.id) : sel.a.delete(a.id)));
            pintar();
        }
    });

    function valor() {
        const r = { modulosIds: [], categoriasIds: [], opcionesIds: [], accionesIds: [] };
        for (const m of modulos) {
            if (!sel.m.has(m.id)) continue;
            let conCategoria = false;
            for (const c of m.categorias || []) {
                const ops = (c.opciones || []).filter((o) => sel.o.has(o.id));
                if (!ops.length) continue;
                conCategoria = true;
                r.categoriasIds.push(c.id);
                for (const o of ops) {
                    r.opcionesIds.push(o.id);
                    for (const a of o.acciones || []) if (a.clave === "ver" || sel.a.has(a.id)) r.accionesIds.push(a.id);
                }
            }
            if (conCategoria || protegido) r.modulosIds.push(m.id);
        }
        return r;
    }

    pintar();
    return { valor, vacio: () => !valor().opcionesIds.length };
}

export function resumenRol(rol) {
    const opciones = (rol.opcionesIds || []).length;
    return [["Módulos", (rol.modulosIds || []).length], ["Opciones", opciones], ["Acciones", Math.max(0, (rol.accionesIds || []).length - opciones)]];
}
