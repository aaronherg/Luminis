import { area, activarTooltips, dona, fechaCorta } from "../../../comunes/graficos.js?v=20261122-4";

let ui;
let root;
let timer;
let globo;
let ritmo = 1.2;
let anterior = null;

export function render() {
    return `
        <section class="lm-live-hero">
            <div class="lm-live-copy">
                <p class="consumo-eyebrow">Red Luminis en vivo</p>
                <p class="lm-live-big"><span data-live="conexiones">0</span><small>conexiones WebSocket abiertas ahora</small></p>
                <div class="lm-live-chips">
                    <span>Usuarios en línea <b data-live="usuarios">0</b></span>
                    <span>Clientes conectados <b data-live="clientes">0</b></span>
                    <span>Eventos/s <b data-live="porSegundo">0</b></span>
                    <span>En cola offline <b data-live="enCola">0</b></span>
                </div>
            </div>
            <div class="lm-live-globe" data-globe-wrap><canvas data-globe aria-label="Tráfico de la red en el globo" role="img"></canvas></div>
        </section>
        <div class="fr-stats" data-stats></div>
        <div class="fr-bento">
            <section class="fr-panel span-8">
                <header class="fr-panel-head"><div><h2>Tráfico · 30 días</h2><p>Peticiones y eventos diarios de toda la red</p></div></header>
                <div class="fr-panel-body" data-traffic></div>
            </section>
            <section class="fr-panel span-4">
                <header class="fr-panel-head"><div><h2>Planes vigentes</h2><p>Clientes por plan</p></div></header>
                <div class="fr-panel-body" data-plans></div>
            </section>
            <section class="fr-panel span-7">
                <header class="fr-panel-head"><div><h2>Ingresos · 6 meses</h2><p>Pagos aplicados por mes</p></div></header>
                <div class="fr-panel-body" data-income></div>
            </section>
            <section class="fr-panel span-5">
                <header class="fr-panel-head"><div><h2>Últimos pagos</h2><p>Movimientos más recientes</p></div></header>
                <div data-payments></div>
            </section>
        </div>`;
}

function dinero(valor) {
    return ui.formato.moneda(valor);
}

function pintarVivo(vivo, porSegundo) {
    const valores = { ...vivo, porSegundo };
    root.querySelectorAll("[data-live]").forEach(elemento => {
        const clave = elemento.dataset.live;
        const valor = Number(valores[clave] || 0);
        elemento.textContent = clave === "porSegundo" ? ui.formato.numero(valor, 1) : ui.formato.numero(valor);
    });
}

async function medirRitmo() {
    try {
        const respuesta = await ResumenService.red();
        const datos = respuesta?.data ?? respuesta ?? {};
        const actual = { total: Number(datos.eventosProcesados || 0), momento: Number(datos.momento || Date.now()) };
        let porSegundo = 0;
        if (anterior && actual.momento > anterior.momento && actual.total >= anterior.total) {
            porSegundo = (actual.total - anterior.total) / ((actual.momento - anterior.momento) / 1000);
        }
        anterior = actual;
        return porSegundo;
    } catch (_) {
        return 0;
    }
}

async function cargar() {
    try {
        const [r, porSegundo] = await Promise.all([ResumenService.administracion(), medirRitmo()]);
        if (!root) return;
        const vivo = r.enVivo || {};
        ritmo = Math.min(7, 1 + Math.log10(1 + porSegundo) * 2.2 + Math.log10(1 + Number(vivo.conexiones || 0)) * 0.6);
        pintarVivo(vivo, porSegundo);
        const entrega = Number(r.mensajes30) ? (100 * Number(r.entregados30 || 0)) / Number(r.mensajes30) : 100;
        root.querySelector("[data-stats]").innerHTML = [
            { titulo: "Peticiones hoy", valor: ui.formato.numero(r.peticionesHoy), iconoNombre: "zap", tono: "is-gold" },
            { titulo: "Eventos hoy", valor: ui.formato.numero(r.mensajesHoy), iconoNombre: "send" },
            { titulo: "Entrega · 30 días", valor: `${ui.formato.numero(Math.min(100, entrega), 1)}%`, iconoNombre: "check", tono: "is-success" },
            { titulo: "Clientes activos", valor: ui.formato.numero(r.clientesActivos), detalle: `${ui.formato.numero(r.suscripcionesVigentes)} con plan vigente`, iconoNombre: "building-2", tono: "is-cyan" },
            { titulo: "Ingresos del mes", valor: dinero(r.ingresosMes), iconoNombre: "wallet", tono: "is-gold" },
            { titulo: "Por cobrar", valor: dinero(r.montoPendiente), detalle: `${ui.formato.numero(r.cargosPendientes)} cargos · ${ui.formato.numero(r.solicitudesPendientes)} solicitudes`, iconoNombre: "receipt" }
        ].map(ui.tarjetaDato).join("");
        root.querySelector("[data-traffic]").innerHTML = area({
            puntos: (r.trafico || []).map(x => ({ etiqueta: x.fecha, valor: Number(x.peticiones || 0), secundario: Number(x.mensajes || 0) })),
            etiqueta: p => fechaCorta(p.etiqueta),
            formato: v => ui.formato.numero(v),
            secundaria: { nombre: "Eventos", formato: v => ui.formato.numero(v) }
        });
        const ingresos = new Map((r.ingresosMensuales || []).map(x => [x.mes, Number(x.monto || 0)]));
        const meses = Array.from({ length: 6 }, (_, i) => {
            const fecha = new Date();
            fecha.setDate(1);
            fecha.setMonth(fecha.getMonth() - 5 + i);
            const clave = fecha.toISOString().slice(0, 7);
            return { etiqueta: clave, valor: ingresos.get(clave) || 0 };
        });
        root.querySelector("[data-income]").innerHTML = area({ puntos: meses, formato: dinero });
        const planes = (r.planes || []).filter(x => Number(x.clientes) > 0);
        root.querySelector("[data-plans]").innerHTML = dona({
            items: planes.map(p => ({ etiqueta: p.nombre, valor: Number(p.clientes) })),
            formato: v => ui.formato.numero(v),
            centro: ui.formato.numero(planes.reduce((total, p) => total + Number(p.clientes), 0))
        });
        root.querySelector("[data-payments]").innerHTML = ui.tabla({
            filas: r.ultimosPagos || [],
            vacio: "Aún no hay pagos registrados.",
            columnas: [
                { titulo: "Cliente", valor: p => `<span class="fr-cell-main">${ui.escapar(p.clienteNombre)}</span><span class="fr-cell-sub">${ui.escapar(p.metodo || "")}</span>` },
                { titulo: "Fecha", valor: p => ui.formato.fechaHora(p.fechaPago) },
                { titulo: "Monto", clase: "is-right", valor: p => `<b>${ui.formato.moneda(p.monto, p.monedaSimbolo || "US$")}</b>` }
            ]
        });
        activarTooltips(root);
    } catch (error) {
        ui?.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    root = stage;
    globo = window.LuminisGlobo?.crear({
        contenedor: stage.querySelector("[data-globe-wrap]"),
        lienzo: stage.querySelector("[data-globe]"),
        mapa: "../files/mapa/tierra.json",
        ritmo: () => ritmo,
        escala: 0.42
    });
    await cargar();
    timer = setInterval(cargar, 10000);
}

export function reset() {
    clearInterval(timer);
    timer = null;
    globo?.destruir();
    globo = null;
    anterior = null;
    root = null;
    ui = null;
}
