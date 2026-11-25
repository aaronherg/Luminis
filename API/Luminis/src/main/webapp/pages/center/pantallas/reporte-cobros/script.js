import { barras, exportarCsv } from "../../../comunes/graficos.js?v=20261122-5";

let ui;
let raiz;
let datos = null;

const COLUMNAS = [
    { titulo: "Cliente", valor: c => c.clienteNombre },
    { titulo: "Plan", valor: c => c.planNombre },
    { titulo: "Vencimiento", valor: c => c.fechaVencimiento },
    { titulo: "Días vencido", valor: c => Math.max(0, Number(c.diasVencido || 0)) },
    { titulo: "Tramo", valor: c => c.tramo },
    { titulo: "Total", valor: c => c.montoTotal },
    { titulo: "Pagado", valor: c => c.montoPagado },
    { titulo: "Pendiente", valor: c => c.montoPendiente }
];

export function render() {
    return `
        <div class="fr-toolbar">
            <p class="fr-toolbar-text">Cargos pendientes de todos los clientes, agrupados por antigüedad de la deuda.</p>
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn" type="button" data-recargar>${LuminisPanel.icono("refresh")}<span>Actualizar</span></button>
            <button class="fr-btn" type="button" data-exportar>${LuminisPanel.icono("download")}<span>Exportar CSV</span></button>
        </div>
        <div class="fr-stats" data-stats></div>
        <div class="fr-bento">
            <section class="fr-panel span-5">
                <header class="fr-panel-head"><div><h2>Antigüedad</h2><p>Monto pendiente por tramo</p></div></header>
                <div class="fr-panel-body" data-tramos></div>
            </section>
            <section class="fr-panel span-7">
                <header class="fr-panel-head"><div><h2>Clientes con mayor deuda</h2><p>Top 8 por monto pendiente</p></div></header>
                <div class="fr-panel-body" data-top></div>
            </section>
        </div>
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Cargos pendientes</h2></div></header>
            <div data-detalle></div>
        </section>`;
}

function badgeTramo(cargo) {
    const dias = Number(cargo.diasVencido || 0);
    const clase = dias <= 0 ? "is-info" : dias <= 30 ? "is-warning" : "is-danger";
    return `<span class="fr-badge ${clase}">${ui.escapar(cargo.tramo)}</span>`;
}

async function cargar() {
    try {
        datos = await ReporteService.cuentasPorCobrar();
        raiz.querySelector("[data-stats]").innerHTML = [
            ui.tarjetaDato({ titulo: "Total por cobrar", valor: ui.formato.moneda(datos.total), iconoNombre: "wallet", tono: "is-accent" }),
            ui.tarjetaDato({ titulo: "Vencido", valor: ui.formato.moneda(datos.vencido), iconoNombre: "ban" }),
            ui.tarjetaDato({ titulo: "Cargos pendientes", valor: ui.formato.numero(datos.cargos), iconoNombre: "receipt" })
        ].join("");
        raiz.querySelector("[data-tramos]").innerHTML = barras({
            items: (datos.tramos || []).map(t => ({ etiqueta: t.tramo, valor: Number(t.monto || 0) })),
            formato: v => ui.formato.moneda(v)
        });
        const porCliente = new Map();
        (datos.detalle || []).forEach(c => porCliente.set(c.clienteNombre, (porCliente.get(c.clienteNombre) || 0) + Number(c.montoPendiente || 0)));
        const top = [...porCliente.entries()].sort((a, b) => b[1] - a[1]).slice(0, 8);
        raiz.querySelector("[data-top]").innerHTML = top.length
            ? barras({ items: top.map(([etiqueta, valor]) => ({ etiqueta, valor })), formato: v => ui.formato.moneda(v) })
            : `<div class="fr-empty">${ui.icono("check")}<p>No hay deudas pendientes.</p></div>`;
        raiz.querySelector("[data-detalle]").innerHTML = ui.tabla({
            filas: datos.detalle || [],
            vacio: "No hay cargos pendientes.",
            columnas: [
                { titulo: "Cliente", valor: c => `<span class="fr-cell-main" data-i18n-skip>${ui.escapar(c.clienteNombre)}</span><span class="fr-cell-sub">${ui.escapar(c.planNombre)}</span>` },
                { titulo: "Periodo", valor: c => `${ui.formato.fecha(c.fechaInicio)} – ${ui.formato.fecha(c.fechaFin)}` },
                { titulo: "Vence", valor: c => ui.formato.fecha(c.fechaVencimiento) },
                { titulo: "Antigüedad", valor: badgeTramo },
                { titulo: "Pagado", clase: "is-right", valor: c => ui.formato.moneda(c.montoPagado, c.monedaSimbolo) },
                { titulo: "Pendiente", clase: "is-right", valor: c => `<strong>${ui.formato.moneda(c.montoPendiente, c.monedaSimbolo)}</strong>` }
            ]
        });
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    raiz.querySelector("[data-recargar]").addEventListener("click", cargar);
    raiz.querySelector("[data-exportar]").addEventListener("click", () => {
        if (datos) exportarCsv("cuentas_por_cobrar", COLUMNAS, datos.detalle || []);
    });
    await cargar();
}

export function reset() {
    datos = null;
}

export function skeleton() {
    return window.LuminisSk.tablero({ tarjetas: 4, columnas: 5, filtros: true });
}
