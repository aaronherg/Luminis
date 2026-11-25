let ui;
let raiz;
let planes = [];
let solicitudes = [];

export function render() {
    return `
        <section class="fr-panel" data-vigente></section>
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Planes disponibles</h2><p>Solicita el plan que necesitas. Nuestro equipo lo activará y generará el cargo.</p></div></header>
            <div class="fr-panel-body"><div class="plan-choices" data-planes></div></div>
        </section>
        <div class="fr-grid-2">
            <section class="fr-panel">
                <header class="fr-panel-head"><div><h2>Mis solicitudes</h2></div></header>
                <div data-solicitudes></div>
            </section>
            <section class="fr-panel">
                <header class="fr-panel-head"><div><h2>Historial de planes</h2></div></header>
                <div data-historial></div>
            </section>
        </div>`;
}

function pintarVigente(datos) {
    const vigente = datos?.vigente;
    const usado = Number(datos?.peticionesUsadas || 0);
    const limite = Number(datos?.peticionesLimite || 0);
    const ilimitadas = Boolean(datos?.peticionesIlimitadas);
    const porcentaje = limite ? Math.min(100, (usado / limite) * 100) : 0;
    raiz.querySelector("[data-vigente]").innerHTML = vigente
        ? `
            <header class="fr-panel-head"><div><h2>Plan ${ui.escapar(vigente.planNombre)}</h2><p>${vigente.tipoCobro === "POSTPAGO" ? "Pospago con renovación automática" : "Prepago"}${vigente.permitirAdicionales ? " · Peticiones adicionales activas" : ""}</p></div>${ui.estado(vigente.estado)}</header>
            <div class="fr-panel-body">
                <dl class="fr-kv">
                    <div><dt>Peticiones</dt><dd>${ui.formato.numero(usado)}${ilimitadas ? " · Ilimitadas" : ` de ${ui.formato.numero(limite)}`}</dd></div>
                    <div><dt>Conexiones simultáneas</dt><dd>${datos.conexionesIlimitadas ? "Ilimitadas" : ui.formato.numero(datos.conexionesLimite)}</dd></div>
                    <div><dt>Mensaje máximo</dt><dd>${vigente.tamanoMaximoMensajeKb == null ? "Sin límite" : `${ui.formato.numero(vigente.tamanoMaximoMensajeKb)} KB`}</dd></div>
                    <div><dt>Retención offline</dt><dd>${ui.formato.numero(vigente.retencionSegundos)} s</dd></div>
                    <div><dt>Periodo</dt><dd>${ui.formato.fecha(vigente.fechaInicio)} – ${ui.formato.fecha(vigente.fechaFin)}</dd></div>
                    <div><dt>Precio</dt><dd>${ui.formato.moneda(vigente.precio, vigente.monedaSimbolo)}${Number(vigente.precioMilAdicional) > 0 ? ` · ${ui.formato.moneda(vigente.precioMilAdicional, vigente.monedaSimbolo)} / 1,000 extra` : ""}</dd></div>
                </dl>
                ${ilimitadas ? "" : `<div class="fr-meter ${porcentaje < 85 ? "" : "is-alert"}" style="margin-top:18px"><span style="width:${porcentaje}%"></span></div>`}
            </div>`
        : `<div class="fr-empty">${ui.icono("gauge")}<p>No tienes un plan vigente. Elige un plan para conectar tu aplicación.</p></div>`;
}

function pintarPlanes(vigente) {
    const pendiente = solicitudes.some(s => s.estado === "Pendiente");
    const puedeSolicitar = ui.puede("suscripcion", "solicitar");
    let sugerido = null;
    try {
        sugerido = sessionStorage.getItem("luminis.planElegido");
    } catch (_) {
        sugerido = null;
    }
    raiz.querySelector("[data-planes]").innerHTML = planes.length
        ? planes
              .map(
                  p => `
            <article class="plan-choice ${String(p.planId) === sugerido ? "is-suggested" : ""} ${p.destacado ? "is-featured" : ""}">
                <header><h3>${ui.escapar(p.nombre)}</h3>${vigente?.planId === p.planId ? '<span class="fr-badge is-success">Actual</span>' : ""}</header>
                <p class="plan-choice-price">${ui.formato.moneda(p.precio, p.monedaSimbolo)} <span>/ ${ui.escapar(p.modalidad)}</span></p>
                <ul>
                    <li>${p.peticionesMes == null ? "Peticiones ilimitadas" : `${ui.formato.numero(p.peticionesMes)} peticiones al mes`}</li>
                    <li>${p.conexionesSimultaneas == null ? "Conexiones ilimitadas" : `${ui.formato.numero(p.conexionesSimultaneas)} conexiones simultáneas`}</li>
                    <li>${p.tamanoMaximoMensajeKb == null ? "Sin límite de mensaje" : `${ui.formato.numero(p.tamanoMaximoMensajeKb)} KB por mensaje`} · ${ui.formato.numero(p.retencionSegundos)} s de retención</li>
                    ${p.peticionesMes != null && Number(p.precioMilAdicional) > 0 ? `<li>+${ui.formato.moneda(p.precioMilAdicional, p.monedaSimbolo)} por 1,000 peticiones extra</li>` : ""}
                </ul>
                ${puedeSolicitar ? `<button class="fr-btn ${p.destacado ? "fr-btn-primary" : ""}" type="button" data-solicitar="${p.planId}" ${pendiente ? "disabled" : ""}>Solicitar</button>` : ""}
            </article>`
              )
              .join("")
        : `<div class="fr-empty">${ui.icono("layers")}<p>No hay planes publicados por el momento.</p></div>`;
    if (pendiente && puedeSolicitar) {
        raiz.querySelector("[data-planes]").insertAdjacentHTML("beforebegin", '<p class="fr-note" data-aviso-pendiente>Tienes una solicitud pendiente. Podrás solicitar otro plan cuando sea respondida o si la cancelas.</p>');
    }
}

function pintarSolicitudes() {
    raiz.querySelector("[data-solicitudes]").innerHTML = ui.tabla({
        filas: solicitudes,
        clave: "suscripcionSolicitudId",
        vacio: "No has enviado solicitudes.",
        columnas: [
            { titulo: "Plan", valor: s => `<span class="fr-cell-main">${ui.escapar(s.planNombre)}</span><span class="fr-cell-sub">${ui.escapar(s.tipoCobro)} · ${ui.formato.fecha(s.fechaCreacion)}</span>` },
            { titulo: "Estado", valor: s => `${ui.estado(s.estado)}${s.respuesta ? `<span class="fr-cell-sub">${ui.escapar(s.respuesta)}</span>` : ""}` },
            {
                titulo: "Acciones",
                clase: "is-actions",
                valor: s => (s.estado === "Pendiente" && ui.puede("suscripcion", "solicitar") ? ui.botonAccion("cancelar", "x", "Cancelar solicitud", "is-danger") : "")
            }
        ]
    });
}

async function cargar() {
    try {
        raiz.querySelector("[data-aviso-pendiente]")?.remove();
        const [datos, publicos, respuesta] = await Promise.all([
            SuscripcionService.obtener(),
            PlanService.publicos(),
            SuscripcionSolicitudService.paginar({ page: 1, size: 10 })
        ]);
        planes = publicos || [];
        solicitudes = respuesta?.records || [];
        pintarVigente(datos);
        pintarPlanes(datos?.vigente);
        pintarSolicitudes();
        raiz.querySelector("[data-historial]").innerHTML = ui.tabla({
            filas: datos?.historial || [],
            vacio: "Sin historial.",
            columnas: [
                { titulo: "Plan", valor: d => `<span class="fr-cell-main">${ui.escapar(d.planNombre)}</span><span class="fr-cell-sub">${ui.escapar(d.tipoCobro)}</span>` },
                { titulo: "Periodo", valor: d => `${ui.formato.fecha(d.fechaInicio)} – ${ui.formato.fecha(d.fechaFin)}` },
                { titulo: "Estado", valor: d => ui.estado(d.estado) }
            ]
        });
    } catch (error) {
        ui.notificarError(error);
    }
}

function solicitar(plan) {
    ui.formulario({
        titulo: `Solicitar plan ${plan.nombre}`,
        subtitulo: `${ui.formato.moneda(plan.precio, plan.monedaSimbolo)} / ${plan.modalidad}`,
        ancho: "sm",
        textoGuardar: "Enviar solicitud",
        campos: `
            ${ui.campo({
                nombre: "tipoCobro",
                etiqueta: "Modalidad de pago",
                tipo: "select",
                valor: "PREPAGO",
                clase: "is-full",
                opciones: [
                    { valor: "PREPAGO", texto: "Prepago: pago el periodo por adelantado" },
                    { valor: "POSTPAGO", texto: "Pospago: se renueva automáticamente" }
                ]
            })}
            ${ui.campo({ nombre: "comentario", etiqueta: "Comentario (opcional)", tipo: "textarea", max: 500, clase: "is-full" })}`,
        async alGuardar(form) {
            const datos = ui.valores(form);
            await SuscripcionSolicitudService.solicitar({ planId: plan.planId, tipoCobro: datos.tipoCobro, comentario: datos.comentario });
            try {
                sessionStorage.removeItem("luminis.planElegido");
            } catch (_) {
                datos.limpio = true;
            }
            AppNotifications.success("Solicitud enviada correctamente");
            await cargar();
        }
    });
}

async function cancelar(solicitud) {
    const ok = await ui.confirmar({
        titulo: "Cancelar solicitud",
        mensaje: `Se cancelará tu solicitud del plan ${solicitud.planNombre}.`,
        textoConfirmar: "Cancelar solicitud",
        peligro: true
    });
    if (!ok) return;
    try {
        await SuscripcionSolicitudService.cancelar(solicitud.suscripcionSolicitudId);
        AppNotifications.success("Solicitud cancelada correctamente");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    raiz.querySelector("[data-planes]").addEventListener("click", event => {
        const boton = event.target.closest("[data-solicitar]");
        if (!boton) return;
        const plan = planes.find(p => String(p.planId) === boton.dataset.solicitar);
        if (plan) solicitar(plan);
    });
    raiz.querySelector("[data-solicitudes]").addEventListener("click", event => {
        const boton = event.target.closest("[data-action='cancelar']");
        if (!boton) return;
        const solicitud = solicitudes.find(s => String(s.suscripcionSolicitudId) === boton.closest("[data-row]")?.dataset.row);
        if (solicitud) cancelar(solicitud);
    });
    await cargar();
}

export function reset() {
    planes = [];
    solicitudes = [];
}

export function skeleton() {
    return window.LuminisSk.dividido(window.LuminisSk.formulario({ campos: 6 }), window.LuminisSk.formulario({ campos: 3 }));
}
