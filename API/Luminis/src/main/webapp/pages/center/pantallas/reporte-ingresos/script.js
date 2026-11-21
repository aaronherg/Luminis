import { area, activarTooltips, dona, diasEntre, enlazarFiltros, exportarCsv, fechaCorta, filtrosFecha, hoyIso } from "../../../comunes/graficos.js?v=20261122-4";

let ui;
let raiz;
let datos = null;

const COLUMNAS = [
    { titulo: "Fecha", valor: p => p.fechaPago },
    { titulo: "Cliente", valor: p => p.clienteNombre },
    { titulo: "Plan", valor: p => p.planNombre },
    { titulo: "Método", valor: p => p.metodo },
    { titulo: "Referencia", valor: p => p.referencia || "" },
    { titulo: "Monto", valor: p => p.monto }
];

export function render() {
    return `
        ${filtrosFecha({ desde: hoyIso(29), hasta: hoyIso(0) })}
        <div class="fr-stats" data-stats></div>
        <div class="fr-bento">
            <section class="fr-panel span-8">
                <header class="fr-panel-head"><div><h2>Ingresos por día</h2><p>Abonos registrados en el periodo</p></div></header>
                <div class="fr-panel-body" data-serie></div>
            </section>
            <section class="fr-panel span-4">
                <header class="fr-panel-head"><div><h2>Por método</h2><p>Distribución del monto cobrado</p></div></header>
                <div class="fr-panel-body" data-metodos></div>
            </section>
        </div>
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Detalle de pagos</h2><p>Hasta 1,000 registros del periodo</p></div></header>
            <div data-detalle></div>
        </section>`;
}

async function generar(filtro) {
    try {
        datos = await ReporteService.ingresos(filtro);
        const dias = diasEntre(datos.desde, datos.hasta);
        const porDia = new Map((datos.porDia || []).map(d => [String(d.fecha).slice(0, 10), d]));
        const promedio = dias.length ? Number(datos.total || 0) / dias.length : 0;
        raiz.querySelector("[data-stats]").innerHTML = [
            ui.tarjetaDato({ titulo: "Total cobrado", valor: ui.formato.moneda(datos.total), iconoNombre: "dollar", tono: "is-accent" }),
            ui.tarjetaDato({ titulo: "Pagos registrados", valor: ui.formato.numero(datos.pagos), iconoNombre: "receipt" }),
            ui.tarjetaDato({ titulo: "Promedio diario", valor: ui.formato.moneda(promedio), iconoNombre: "trend" }),
            ui.tarjetaDato({ titulo: "Días del periodo", valor: ui.formato.numero(dias.length), iconoNombre: "calendar" })
        ].join("");
        raiz.querySelector("[data-serie]").innerHTML = area({
            puntos: dias.map(d => ({ etiqueta: d, valor: Number(porDia.get(d)?.monto || 0) })),
            etiqueta: p => fechaCorta(p.etiqueta),
            formato: v => ui.formato.moneda(v)
        });
        raiz.querySelector("[data-metodos]").innerHTML = (datos.porMetodo || []).length
            ? dona({
                  items: datos.porMetodo.map(m => ({ etiqueta: m.metodo, valor: Number(m.monto || 0) })),
                  formato: v => ui.formato.moneda(v),
                  centro: `${datos.porMetodo.length}`
              })
            : `<div class="fr-empty">${ui.icono("pie-chart")}<p>Sin pagos en el periodo.</p></div>`;
        raiz.querySelector("[data-detalle]").innerHTML = ui.tabla({
            filas: datos.detalle || [],
            vacio: "No hay pagos en el periodo seleccionado.",
            columnas: [
                { titulo: "Fecha", valor: p => ui.formato.fechaHora(p.fechaPago) },
                { titulo: "Cliente", valor: p => `<span class="fr-cell-main" data-i18n-skip>${ui.escapar(p.clienteNombre)}</span><span class="fr-cell-sub">${ui.escapar(p.planNombre)}</span>` },
                { titulo: "Método", valor: p => ui.escapar(p.metodo) },
                { titulo: "Referencia", valor: p => `<span data-i18n-skip>${ui.escapar(p.referencia || "—")}</span>` },
                { titulo: "Monto", clase: "is-right", valor: p => `<strong>${ui.formato.moneda(p.monto, p.monedaSimbolo)}</strong>` }
            ]
        });
        activarTooltips(raiz);
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    enlazarFiltros(raiz, generar, () => {
        if (!datos) return;
        exportarCsv(`ingresos_${datos.desde}_${datos.hasta}`, COLUMNAS, datos.detalle || []);
    });
    await generar({ desde: hoyIso(29), hasta: hoyIso(0) });
}

export function reset() {
    datos = null;
}
