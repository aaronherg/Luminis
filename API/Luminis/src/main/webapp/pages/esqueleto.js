(() => {
    const b = (css) => `<div class="fr-skeleton" style="${css}"></div>`;
    const anchos = [78, 62, 90, 55, 70, 84, 48, 66];

    function tabla({ columnas = 5, filas = 8 } = {}) {
        const cols = `repeat(${columnas}, minmax(0, 1fr))`;
        const encabezado = `<div style="display:grid;grid-template-columns:${cols};gap:18px;padding:16px 18px;border-bottom:1px solid var(--line)">${Array.from({ length: columnas }, () => b("height:11px;width:65%")).join("")}</div>`;
        const fila = (i) => `<div style="display:grid;grid-template-columns:${cols};gap:18px;padding:15px 18px;border-bottom:1px solid var(--line)">${Array.from({ length: columnas }, (_, c) => b("height:12px;width:" + anchos[(i + c) % anchos.length] + "%")).join("")}</div>`;
        return encabezado + Array.from({ length: filas }, (_, i) => fila(i)).join("");
    }

    function toolbar({ filtros = 1, accion = true } = {}) {
        return `<div class="fr-toolbar">${b("height:40px;width:min(300px,60%);border-radius:12px")}${Array.from({ length: filtros }, () => b("height:40px;width:150px;border-radius:12px")).join("")}<span class="fr-toolbar-spacer"></span>${accion ? b("height:40px;width:150px;border-radius:12px") : ""}</div>`;
    }

    function lista(opts = {}) {
        return `${toolbar(opts)}<section class="fr-panel">${tabla(opts)}<div style="padding:14px 18px">${b("height:12px;width:160px")}</div></section>`;
    }

    function stats(n = 4) {
        return `<div class="fr-stats">${Array.from({ length: n }, () => `<article class="fr-stat" style="display:grid;gap:10px;padding:18px;border:1px solid var(--line);border-radius:16px;background:var(--surface)">${b("height:30px;width:30px;border-radius:9px")}${b("height:24px;width:60%")}${b("height:11px;width:80%")}</article>`).join("")}</div>`;
    }

    function panelCabecera(tituloAncho = 150) {
        return `<header class="fr-panel-head"><div style="display:grid;gap:6px">${b("height:16px;width:" + tituloAncho + "px")}${b("height:10px;width:220px")}</div></header>`;
    }

    function tablero({ tarjetas = 4, columnas = 5, filtros = false } = {}) {
        const filtro = filtros ? `<div class="fr-toolbar">${b("height:40px;width:180px;border-radius:12px")}${b("height:40px;width:180px;border-radius:12px")}<span class="fr-toolbar-spacer"></span></div>` : "";
        const grafico = (span, h) => `<section class="fr-panel ${span}">${panelCabecera()}<div class="fr-panel-body">${b("height:" + h + "px;border-radius:14px")}</div></section>`;
        return `${filtro}${stats(tarjetas)}<div class="fr-bento">${grafico("span-8", 250)}${grafico("span-4", 250)}</div><section class="fr-panel">${panelCabecera(170)}${tabla({ columnas, filas: 6 })}</section>`;
    }

    function campo(ancho = "100%") {
        return `<div style="display:grid;gap:7px;width:${ancho}">${b("height:12px;width:130px")}${b("height:44px;border-radius:12px")}</div>`;
    }

    function formulario({ titulo = "Formulario", campos = 6, columnas = 2, accion = true } = {}) {
        const grid = `<div style="display:grid;grid-template-columns:repeat(${columnas},minmax(0,1fr));gap:16px">${Array.from({ length: campos }, () => campo()).join("")}</div>`;
        return `<section class="fr-panel">${panelCabecera(titulo.length * 9)}<div class="fr-panel-body" style="display:grid;gap:16px">${grid}${accion ? b("height:42px;width:160px;border-radius:12px;justify-self:end") : ""}</div></section>`;
    }

    function dividido(izquierda, derecha) {
        return `<div class="fr-grid-2">${izquierda}${derecha}</div>`;
    }

    window.LuminisSk = Object.freeze({ b, tabla, toolbar, lista, stats, tablero, formulario, campo, panelCabecera, dividido });
})();
