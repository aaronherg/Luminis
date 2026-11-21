let ui;
let raiz;
let vista = "suscripciones";
let pagina = 1;
let buscar = "";
let estadoSolicitud = "Pendiente";
let registros = [];
let planes = [];
let temporizador = 0;

export function render() {
    return `
        <div class="fr-tabs" role="tablist">
            <button type="button" role="tab" data-vista="suscripciones">Suscripciones</button>
            <button type="button" role="tab" data-vista="solicitudes">Solicitudes<span class="fr-count" data-pendientes hidden></span></button>
        </div>
        <div class="fr-toolbar" data-toolbar></div>
        <section class="fr-panel"><div data-lista></div><div data-paginador></div></section>`;
}

function pintarToolbar() {
    raiz.querySelectorAll("[data-vista]").forEach(b => {
        b.classList.toggle("is-active", b.dataset.vista === vista);
        b.setAttribute("aria-selected", String(b.dataset.vista === vista));
    });
    raiz.querySelector("[data-toolbar]").innerHTML =
        vista === "suscripciones"
            ? `<label class="fr-search">${LuminisPanel.icono("search")}<input class="fr-input" type="search" placeholder="Buscar cliente" data-buscar value="${ui.escapar(buscar)}" aria-label="Buscar cliente"></label>`
            : `<select class="fr-select" data-estado-solicitud aria-label="Estado de la solicitud">
                   ${["Pendiente", "Aprobada", "Rechazada", "Cancelada", ""].map(e => `<option value="${e}" ${e === estadoSolicitud ? "selected" : ""}>${e || "Todas"}</option>`).join("")}
               </select>`;
}

function descripcionVigente(v) {
    if (!v) return '<span class="fr-badge is-warning">Sin plan vigente</span>';
    return `<span class="fr-cell-main">${ui.escapar(v.planNombre)}</span><span class="fr-cell-sub">${ui.escapar(v.tipoCobro)}${v.permitirAdicionales ? " · con adicionales" : ""}${v.renovacionAutomatica ? " · renovación automática" : ""}</span>`;
}

async function cargarSuscripciones() {
    const respuesta = await SuscripcionService.paginar({ page: pagina, size: 15, buscar: buscar || null });
    registros = respuesta?.records || [];
    raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
        filas: registros,
        clave: "clienteId",
        vacio: "No hay suscripciones para mostrar.",
        columnas: [
            { titulo: "Cliente", valor: s => `<span class="fr-cell-main">${ui.escapar(s.clienteNombre)}</span>` },
            { titulo: "Plan vigente", valor: s => descripcionVigente(s.vigente) },
            { titulo: "Periodo", valor: s => (s.vigente ? `${ui.formato.fecha(s.vigente.fechaInicio)} – ${ui.formato.fecha(s.vigente.fechaFin)}` : "—") },
            {
                titulo: "Precio",
                clase: "is-right",
                valor: s => (s.vigente ? ui.formato.moneda(s.vigente.precio, s.vigente.monedaSimbolo) : "—")
            },
            {
                titulo: "Acciones",
                clase: "is-actions",
                valor: s =>
                    [
                        ui.puede("suscripciones", "asignar") ? ui.botonAccion("asignar", "layers", s.vigente ? "Cambiar plan" : "Asignar plan") : "",
                        ui.puede("suscripciones", "cancelar") && s.vigente ? ui.botonAccion("cancelar", "ban", "Cancelar plan", "is-danger") : ""
                    ].join("")
            }
        ]
    });
    raiz.querySelector("[data-paginador]").innerHTML = ui.paginador(respuesta);
}

async function cargarSolicitudes() {
    const respuesta = await SuscripcionSolicitudService.paginar({ page: pagina, size: 15, estado: estadoSolicitud || null });
    registros = respuesta?.records || [];
    const gestionar = ui.puede("suscripciones", "gestionar_solicitudes");
    raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
        filas: registros,
        clave: "suscripcionSolicitudId",
        vacio: "No hay solicitudes para mostrar.",
        columnas: [
            { titulo: "Cliente", valor: s => `<span class="fr-cell-main">${ui.escapar(s.clienteNombre)}</span><span class="fr-cell-sub">${ui.formato.fechaHora(s.fechaCreacion)}</span>` },
            { titulo: "Plan solicitado", valor: s => `<span class="fr-cell-main">${ui.escapar(s.planNombre)}</span><span class="fr-cell-sub">${ui.escapar(s.tipoCobro)}</span>` },
            { titulo: "Comentario", valor: s => ui.escapar(s.comentario || "—") },
            { titulo: "Estado", valor: s => `${ui.estado(s.estado)}${s.respuesta ? `<span class="fr-cell-sub">${ui.escapar(s.respuesta)}</span>` : ""}` },
            {
                titulo: "Acciones",
                clase: "is-actions",
                valor: s =>
                    gestionar && s.estado === "Pendiente"
                        ? ui.botonAccion("aprobar", "check", "Aprobar") + ui.botonAccion("rechazar", "x", "Rechazar", "is-danger")
                        : ""
            }
        ]
    });
    raiz.querySelector("[data-paginador]").innerHTML = ui.paginador(respuesta);
}

async function contarPendientes() {
    try {
        const respuesta = await SuscripcionSolicitudService.paginar({ page: 1, size: 1, estado: "Pendiente" });
        const total = Number(respuesta?.totalRecords || 0);
        const badge = raiz.querySelector("[data-pendientes]");
        badge.textContent = String(total);
        badge.hidden = total === 0;
    } catch (_) {
        return;
    }
}

async function cargar() {
    try {
        if (vista === "suscripciones") await cargarSuscripciones();
        else await cargarSolicitudes();
    } catch (error) {
        ui.notificarError(error);
    }
}

function opcionesPlanes(seleccionado) {
    return planes
        .filter(p => p.estado === "Activo")
        .map(p => ({ valor: p.planId, texto: `${p.nombre} · ${ui.formato.moneda(p.precio, p.monedaSimbolo)} / ${p.modalidad}` }))
        .map(o => ({ ...o, seleccionado: String(o.valor) === String(seleccionado) }));
}

function asignar(suscripcion) {
    const actual = suscripcion.vigente;
    ui.formulario({
        titulo: actual ? "Cambiar plan" : "Asignar plan",
        subtitulo: suscripcion.clienteNombre,
        ancho: "md",
        textoGuardar: "Asignar plan",
        campos: `
            ${ui.campo({ nombre: "planId", etiqueta: "Plan", tipo: "select", valor: actual?.planId, requerido: true, clase: "is-full", opciones: opcionesPlanes(actual?.planId) })}
            ${ui.campo({
                nombre: "tipoCobro",
                etiqueta: "Tipo de cobro",
                tipo: "select",
                valor: actual?.tipoCobro || "PREPAGO",
                opciones: [
                    { valor: "PREPAGO", texto: "Prepago" },
                    { valor: "POSTPAGO", texto: "Pospago (renovación automática)" }
                ]
            })}
            <div class="fr-field" data-solo-pospago>${ui.campo({ nombre: "permitirAdicionales", etiqueta: "Permitir GB adicionales", tipo: "checkbox", valor: actual?.permitirAdicionales })}<small>Los GB extra se cobran al cierre del periodo.</small></div>
            <fieldset><legend>Pago inicial (opcional)</legend><div class="fr-form">
                ${ui.campo({ nombre: "montoPagado", etiqueta: "Monto recibido", tipo: "number", atributos: 'min="0" step="0.01"' })}
                ${ui.campo({ nombre: "metodoPago", etiqueta: "Método", tipo: "select", valor: "Transferencia", opciones: ["Efectivo", "Transferencia", "Tarjeta", "Deposito", "Otro"].map(m => ({ valor: m, texto: m === "Deposito" ? "Depósito" : m })) })}
                ${ui.campo({ nombre: "referenciaPago", etiqueta: "Referencia", max: 120, clase: "is-full" })}
            </div></fieldset>
            ${actual ? '<p class="fr-note is-accent">El plan vigente se cerrará hoy y comenzará un nuevo periodo con el plan elegido.</p>' : ""}`,
        alAbrir(form) {
            const aplicar = () => {
                const pospago = form.elements.tipoCobro.value === "POSTPAGO";
                form.querySelector("[data-solo-pospago]").hidden = !pospago;
            };
            form.elements.tipoCobro.addEventListener("change", aplicar);
            aplicar();
        },
        async alGuardar(form) {
            ui.exigir(form, [
                ["planId", "Selecciona un plan."],
                ["montoPagado", "El monto debe ser mayor o igual a cero.", valor => !valor || Number(valor) >= 0]
            ]);
            const datos = ui.valores(form);
            await SuscripcionService.asignar({
                clienteId: suscripcion.clienteId,
                planId: Number(datos.planId),
                tipoCobro: datos.tipoCobro,
                permitirAdicionales: datos.tipoCobro === "POSTPAGO" && datos.permitirAdicionales,
                montoPagado: datos.montoPagado,
                metodoPago: datos.metodoPago,
                referenciaPago: datos.referenciaPago
            });
            AppNotifications.success("Plan asignado correctamente");
            await cargar();
        }
    });
}

async function cancelar(suscripcion) {
    const ok = await ui.confirmar({
        titulo: "Cancelar plan",
        mensaje: `El plan ${suscripcion.vigente.planNombre} de ${suscripcion.clienteNombre} terminará hoy y no se renovará. Las conexiones y publicaciones quedarán suspendidas hasta que tenga un plan vigente.`,
        textoConfirmar: "Cancelar plan",
        peligro: true
    });
    if (!ok) return;
    try {
        await SuscripcionService.cancelar(suscripcion.clienteId);
        AppNotifications.success("Plan cancelado correctamente");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

function responder(solicitud, aprobar) {
    ui.formulario({
        titulo: aprobar ? "Aprobar solicitud" : "Rechazar solicitud",
        subtitulo: `${solicitud.clienteNombre} · ${solicitud.planNombre} (${solicitud.tipoCobro})`,
        ancho: "sm",
        textoGuardar: aprobar ? "Aprobar y asignar" : "Rechazar",
        campos: `
            ${aprobar && solicitud.tipoCobro === "POSTPAGO" ? ui.campo({ nombre: "permitirAdicionales", etiqueta: "Permitir GB adicionales", tipo: "checkbox" }) : ""}
            ${ui.campo({ nombre: "respuesta", etiqueta: aprobar ? "Mensaje para el cliente (opcional)" : "Motivo del rechazo", tipo: "textarea", max: 500, clase: "is-full", requerido: !aprobar })}
            ${aprobar ? '<p class="fr-note">Se asignará el plan de inmediato y se generará el cargo correspondiente.</p>' : ""}`,
        async alGuardar(form) {
            if (!aprobar) ui.exigir(form, [["respuesta", "Indica el motivo del rechazo."]]);
            const datos = ui.valores(form);
            await SuscripcionSolicitudService.responder({
                suscripcionSolicitudId: solicitud.suscripcionSolicitudId,
                aprobar,
                permitirAdicionales: Boolean(datos.permitirAdicionales),
                respuesta: datos.respuesta
            });
            AppNotifications.success(aprobar ? "Solicitud aprobada y plan asignado" : "Solicitud rechazada");
            await Promise.all([cargar(), contarPendientes()]);
        }
    });
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    pintarToolbar();
    raiz.querySelectorAll("[data-vista]").forEach(b =>
        b.addEventListener("click", () => {
            vista = b.dataset.vista;
            pagina = 1;
            pintarToolbar();
            cargar();
        })
    );
    raiz.querySelector("[data-toolbar]").addEventListener("input", event => {
        if (!event.target.matches("[data-buscar]")) return;
        window.clearTimeout(temporizador);
        temporizador = window.setTimeout(() => {
            buscar = event.target.value.trim();
            pagina = 1;
            cargar();
        }, 350);
    });
    raiz.querySelector("[data-toolbar]").addEventListener("change", event => {
        if (!event.target.matches("[data-estado-solicitud]")) return;
        estadoSolicitud = event.target.value;
        pagina = 1;
        cargar();
    });
    raiz.querySelector("[data-paginador]").addEventListener("click", event => {
        const boton = event.target.closest("[data-page]");
        if (!boton || boton.disabled) return;
        pagina = Number(boton.dataset.page);
        cargar();
    });
    raiz.querySelector("[data-lista]").addEventListener("click", event => {
        const boton = event.target.closest("[data-action]");
        if (!boton) return;
        const id = boton.closest("[data-row]")?.dataset.row;
        const clave = vista === "suscripciones" ? "clienteId" : "suscripcionSolicitudId";
        const registro = registros.find(r => String(r[clave]) === id);
        if (!registro) return;
        const acciones = {
            asignar: () => asignar(registro),
            cancelar: () => cancelar(registro),
            aprobar: () => responder(registro, true),
            rechazar: () => responder(registro, false)
        };
        acciones[boton.dataset.action]?.();
    });
    try {
        planes = (await PlanService.listar()) || [];
    } catch (error) {
        ui.notificarError(error);
    }
    await Promise.all([cargar(), contarPendientes()]);
}

export function reset() {
    pagina = 1;
    buscar = "";
    window.clearTimeout(temporizador);
}
