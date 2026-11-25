import { area, activarTooltips, fechaCorta } from "../../../comunes/graficos.js?v=20261122-5";

let ui;
let root;
let timer;
let globo;
let ritmo = 1;

export function render() {
    return `
        <section class="lm-live-hero">
            <div class="lm-live-copy">
                <p class="consumo-eyebrow">Tu red en vivo</p>
                <p class="lm-live-big"><span data-live="conexiones">0</span><small>dispositivos y usuarios conectados ahora</small></p>
                <div class="lm-live-chips">
                    <span>Usuarios en línea <b data-live="usuarios">0</b></span>
                    <span>En cola offline <b data-live="enCola">0</b></span>
                    <span>Límite de conexiones <b data-live="limite">—</b></span>
                </div>
            </div>
            <div class="lm-live-globe" data-globe-wrap><canvas data-globe aria-label="Actividad de tu proyecto en el globo" role="img"></canvas></div>
        </section>
        <section class="fr-panel consumo-hero" data-hero></section>
        <div class="fr-stats" data-stats></div>
        <div class="fr-bento">
            <section class="fr-panel span-8">
                <header class="fr-panel-head"><div><h2>Actividad · 30 días</h2><p>Peticiones consumidas y eventos entregados</p></div></header>
                <div class="fr-panel-body" data-chart></div>
            </section>
            <section class="fr-panel span-4">
                <header class="fr-panel-head"><div><h2>Primeros pasos</h2><p>Enciende tu proyecto en minutos</p></div></header>
                <div class="fr-panel-body"><ol class="primeros-pasos" data-steps></ol></div>
            </section>
        </div>`;
}

function textoRetencion(segundos) {
    const valor = Number(segundos || 0);
    if (valor >= 86400) return `${Math.round(valor / 86400)} d`;
    if (valor >= 3600) return `${Math.round(valor / 3600)} h`;
    if (valor >= 60) return `${Math.round(valor / 60)} min`;
    return `${valor} s`;
}

function dibujar(r) {
    const plan = r.vigente || {};
    const usadas = Number(r.peticionesUsadas || 0);
    const ilimitadas = Boolean(r.peticionesIlimitadas);
    const limite = Number(r.peticionesLimite || 0);
    const porcentaje = limite ? Math.min(100, (usadas / limite) * 100) : 0;
    const vivo = r.enVivo || {};
    const conexiones = Number(vivo.conexiones || 0);
    ritmo = Math.min(5, 0.6 + Math.log10(1 + conexiones) * 1.4);
    const vivos = { conexiones, usuarios: vivo.usuarios, enCola: vivo.enCola };
    root.querySelectorAll("[data-live]").forEach(elemento => {
        const clave = elemento.dataset.live;
        elemento.textContent = clave === "limite"
            ? (r.conexionesIlimitadas ? "∞" : ui.formato.numero(r.conexionesLimite))
            : ui.formato.numero(vivos[clave]);
    });
    root.querySelector("[data-hero]").innerHTML = r.vigente
        ? `<div class="consumo-hero-copy">
                <p class="consumo-eyebrow">Plan ${ui.escapar(plan.planNombre || "")}${plan.tipoCobro ? ` · ${ui.escapar(plan.tipoCobro)}` : ""}</p>
                <h2>${ui.formato.numero(usadas)} <span>${ilimitadas ? "peticiones · sin límite" : `de ${ui.formato.numero(limite)} peticiones este periodo`}</span></h2>
                ${ilimitadas ? "" : `<div class="fr-meter ${porcentaje < 85 ? "" : "is-alert"}"><span style="width:${porcentaje}%"></span></div>`}
                <p class="consumo-meta">${plan.tamanoMaximoMensajeKb == null ? "Eventos de cualquier tamaño" : `Eventos de hasta ${ui.formato.numero(plan.tamanoMaximoMensajeKb)} KB`} · Cola offline ${textoRetencion(plan.retencionSegundos)} · Vence ${ui.formato.fecha(plan.fechaFin)}</p>
            </div>
            <div class="consumo-hero-ring" style="--porcentaje:${ilimitadas ? 100 : porcentaje}"><strong>${ilimitadas ? "∞" : `${ui.formato.numero(porcentaje, 0)}%`}</strong><span>${ilimitadas ? "ilimitado" : "usado"}</span></div>`
        : `<div class="consumo-hero-copy">
                <p class="consumo-eyebrow">Sin plan vigente</p>
                <h2>Elige un plan <span>para encender tu socket</span></h2>
                <p class="consumo-meta">Sin un plan activo las conexiones se rechazan con el código 4402.</p>
                <a class="fr-btn fr-btn-gold" href="#/suscripcion">Ver planes</a>
            </div>`;
    const entrega = Number(r.mensajes30) ? (100 * Number(r.entregados30 || 0)) / Number(r.mensajes30) : 100;
    root.querySelector("[data-stats]").innerHTML = [
        { titulo: "Eventos · 30 días", valor: ui.formato.numero(r.mensajes30), iconoNombre: "send", tono: "is-gold" },
        { titulo: "Tasa de entrega", valor: `${ui.formato.numero(Math.min(100, entrega), 1)}%`, iconoNombre: "check", tono: "is-success" },
        { titulo: "Pico de conexiones", valor: ui.formato.numero(r.picoConexiones30), detalle: "últimos 30 días", iconoNombre: "trend", tono: "is-cyan" },
        { titulo: "API keys activas", valor: ui.formato.numero(r.apiKeysActivas), iconoNombre: "key-round" },
        { titulo: "Saldo pendiente", valor: ui.formato.moneda(r.montoPendiente, plan.monedaSimbolo || "US$"), iconoNombre: "wallet" }
    ].map(ui.tarjetaDato).join("");
    const metricas = new Map((r.metricas || []).map(x => [String(x.fecha).slice(0, 10), x]));
    const dias = Array.from({ length: 30 }, (_, i) => {
        const fecha = new Date();
        fecha.setDate(fecha.getDate() - 29 + i);
        return fecha.toISOString().slice(0, 10);
    });
    root.querySelector("[data-chart]").innerHTML = area({
        puntos: dias.map(d => ({ etiqueta: d, valor: Number(metricas.get(d)?.peticiones || 0), secundario: Number(metricas.get(d)?.entregados || 0) })),
        etiqueta: p => fechaCorta(p.etiqueta),
        formato: v => ui.formato.numero(v),
        secundaria: { nombre: "Entregados", formato: v => ui.formato.numero(v) }
    });
    const pasos = [
        ["Activa un plan", Boolean(r.vigente), "#/suscripcion"],
        ["Crea tu primera API key", Number(r.apiKeysActivas) > 0, "#/apikeys"],
        ["Conecta un dispositivo", conexiones > 0 || Number(r.picoConexiones30) > 0, "#/consola"],
        ["Publica tu primer evento", Number(r.mensajes30) > 0, "#/consola"],
        ["Invita a tu equipo", false, "#/usuarios"]
    ];
    root.querySelector("[data-steps]").innerHTML = pasos
        .map(([texto, hecho, enlace]) => `<li class="${hecho ? "is-done" : ""}"><span class="paso-check">${ui.icono(hecho ? "check" : "chevron-right")}</span><a href="${enlace}">${texto}</a></li>`)
        .join("");
    activarTooltips(root);
}

async function cargar() {
    try {
        const respuesta = await ResumenService.cliente();
        if (root) dibujar(respuesta);
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
    root = null;
    ui = null;
}

export function skeleton() {
    return window.LuminisSk.tablero({ tarjetas: 4, columnas: 4 });
}
