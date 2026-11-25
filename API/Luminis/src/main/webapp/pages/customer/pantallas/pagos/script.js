let ui;
let raiz;
let pagina = 1;
let estado = "";
let registros = [];

export function render() {
    return `
        <div class="fr-toolbar">
            <select class="fr-select" data-estado aria-label="Estado del cargo">
                <option value="">Todos los cargos</option>
                <option value="Pendiente">Pendientes</option>
                <option value="Pagado">Pagados</option>
            </select>
            <span class="fr-toolbar-spacer"></span>
            <p class="fr-toolbar-text">Para pagar un cargo, contacta a nuestro equipo con la referencia de tu pago.</p>
        </div>
        <section class="fr-panel"><div data-lista></div><div data-paginador></div></section>`;
}

async function cargar() {
    try {
        const respuesta = await PagoSuscripcionService.paginar({ page: pagina, size: 15, estado: estado || null });
        registros = respuesta?.records || [];
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: registros,
            clave: "pagoSuscripcionId",
            vacio: "No tienes cargos registrados.",
            columnas: [
                { titulo: "Plan", valor: p => `<span class="fr-cell-main">${ui.escapar(p.planNombre)}</span><span class="fr-cell-sub">${ui.escapar(p.tipoCobro)}</span>` },
                { titulo: "Periodo", valor: p => `${ui.formato.fecha(p.fechaInicio)} – ${ui.formato.fecha(p.fechaFin)}` },
                { titulo: "Vence", valor: p => ui.formato.fecha(p.fechaVencimiento) },
                {
                    titulo: "Total",
                    clase: "is-right",
                    valor: p => `<span class="fr-cell-main">${ui.formato.moneda(p.montoTotal, p.monedaSimbolo)}</span>${Number(p.montoAdicionales) > 0 ? `<span class="fr-cell-sub">Incluye ${ui.formato.numero(p.peticionesAdicionales)} peticiones extra</span>` : ""}`
                },
                { titulo: "Pendiente", clase: "is-right", valor: p => `<strong>${ui.formato.moneda(p.montoPendiente, p.monedaSimbolo)}</strong>` },
                { titulo: "Estado", valor: p => ui.estado(p.estado) },
                { titulo: "Acciones", clase: "is-actions", valor: () => ui.botonAccion("ver", "eye", "Ver abonos") }
            ]
        });
        raiz.querySelector("[data-paginador]").innerHTML = ui.paginador(respuesta);
    } catch (error) {
        ui.notificarError(error);
    }
}

async function ver(cargo) {
    const { raiz: modal } = ui.modal({
        titulo: "Detalle del cargo",
        subtitulo: `${cargo.planNombre} · ${ui.formato.fecha(cargo.fechaInicio)} – ${ui.formato.fecha(cargo.fechaFin)}`,
        ancho: "md",
        cuerpo: '<div data-detalle style="min-height:160px"></div>'
    });
    try {
        const detalle = await PagoSuscripcionService.obtener(cargo.pagoSuscripcionId);
        modal.querySelector("[data-detalle]").innerHTML = `
            <dl class="fr-kv">
                <div><dt>Plan</dt><dd>${ui.formato.moneda(detalle.montoPlan, detalle.monedaSimbolo)}</dd></div>
                <div><dt>Peticiones adicionales</dt><dd>${ui.formato.numero(detalle.peticionesAdicionales)} · ${ui.formato.moneda(detalle.montoAdicionales, detalle.monedaSimbolo)}</dd></div>
                <div><dt>Total</dt><dd>${ui.formato.moneda(detalle.montoTotal, detalle.monedaSimbolo)}</dd></div>
                <div><dt>Pagado</dt><dd>${ui.formato.moneda(detalle.montoPagado, detalle.monedaSimbolo)}</dd></div>
            </dl>
            <h3 class="fr-subtitle">Abonos</h3>
            ${ui.tabla({
                filas: detalle.detalles || [],
                vacio: "Aún no hay abonos registrados.",
                columnas: [
                    { titulo: "Fecha", valor: d => ui.formato.fechaHora(d.fechaPago) },
                    { titulo: "Método", valor: d => ui.escapar(d.metodo) },
                    { titulo: "Referencia", valor: d => ui.escapar(d.referencia || "—") },
                    { titulo: "Monto", clase: "is-right", valor: d => `<strong>${ui.formato.moneda(d.monto, detalle.monedaSimbolo)}</strong>` }
                ]
            })}`;
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    raiz.querySelector("[data-estado]").addEventListener("change", event => {
        estado = event.target.value;
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
        const boton = event.target.closest("[data-action='ver']");
        if (!boton) return;
        const cargo = registros.find(p => String(p.pagoSuscripcionId) === boton.closest("[data-row]")?.dataset.row);
        if (cargo) ver(cargo);
    });
    await cargar();
}

export function reset() {
    pagina = 1;
    estado = "";
}

export function skeleton() {
    return window.LuminisSk.lista({ filtros: 2, columnas: 5 });
}
