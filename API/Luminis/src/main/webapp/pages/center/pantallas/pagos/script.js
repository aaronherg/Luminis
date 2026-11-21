let ui;
let raiz;
let pagina = 1;
let buscar = "";
let estado = "Pendiente";
let registros = [];
let temporizador = 0;

const METODOS = [
    { valor: "Transferencia", texto: "Transferencia" },
    { valor: "Efectivo", texto: "Efectivo" },
    { valor: "Tarjeta", texto: "Tarjeta" },
    { valor: "Deposito", texto: "Depósito" },
    { valor: "Otro", texto: "Otro" }
];

export function render() {
    return `
        <div class="fr-toolbar">
            <label class="fr-search">${LuminisPanel.icono("search")}<input class="fr-input" type="search" placeholder="Buscar cliente o plan" data-buscar aria-label="Buscar pagos"></label>
            <select class="fr-select" data-estado aria-label="Estado del cargo">
                <option value="Pendiente">Pendientes</option>
                <option value="Pagado">Pagados</option>
                <option value="">Todos</option>
            </select>
        </div>
        <section class="fr-panel"><div data-lista></div><div data-paginador></div></section>`;
}

async function cargar() {
    try {
        const respuesta = await PagoSuscripcionService.paginar({ page: pagina, size: 15, buscar: buscar || null, estado: estado || null });
        registros = respuesta?.records || [];
        const registrar = ui.puede("pagos", "registrar");
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: registros,
            clave: "pagoSuscripcionId",
            vacio: "No hay cargos para mostrar.",
            columnas: [
                { titulo: "Cliente", valor: p => `<span class="fr-cell-main">${ui.escapar(p.clienteNombre)}</span><span class="fr-cell-sub">${ui.escapar(p.planNombre)} · ${ui.escapar(p.tipoCobro)}</span>` },
                { titulo: "Periodo", valor: p => `${ui.formato.fecha(p.fechaInicio)} – ${ui.formato.fecha(p.fechaFin)}` },
                { titulo: "Vence", valor: p => ui.formato.fecha(p.fechaVencimiento) },
                {
                    titulo: "Total",
                    clase: "is-right",
                    valor: p => `<span class="fr-cell-main">${ui.formato.moneda(p.montoTotal, p.monedaSimbolo)}</span>${Number(p.montoAdicionales) > 0 ? `<span class="fr-cell-sub">Incluye ${ui.formato.numero(p.peticionesAdicionales)} peticiones extra</span>` : ""}`
                },
                { titulo: "Pendiente", clase: "is-right", valor: p => `<strong>${ui.formato.moneda(p.montoPendiente, p.monedaSimbolo)}</strong>` },
                { titulo: "Estado", valor: p => ui.estado(p.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: p =>
                        ui.botonAccion("ver", "eye", "Ver abonos") +
                        (registrar && p.estado !== "Pagado" ? ui.botonAccion("pagar", "dollar", "Registrar pago") : "")
                }
            ]
        });
        raiz.querySelector("[data-paginador]").innerHTML = ui.paginador(respuesta);
    } catch (error) {
        ui.notificarError(error);
    }
}

function pagar(cargo) {
    const pendiente = Number(cargo.montoPendiente || 0);
    ui.formulario({
        titulo: "Registrar pago",
        subtitulo: `${cargo.clienteNombre} · ${cargo.planNombre}`,
        ancho: "sm",
        textoGuardar: "Registrar pago",
        campos: `
            <p class="fr-note is-accent">Pendiente: <strong>${ui.formato.moneda(pendiente, cargo.monedaSimbolo)}</strong>. Puedes registrar abonos parciales.</p>
            ${ui.campo({ nombre: "monto", etiqueta: "Monto", tipo: "number", valor: pendiente.toFixed(2), requerido: true, atributos: `min="0.01" max="${pendiente.toFixed(2)}" step="0.01"` })}
            ${ui.campo({ nombre: "metodo", etiqueta: "Método", tipo: "select", valor: "Transferencia", opciones: METODOS })}
            ${ui.campo({ nombre: "referencia", etiqueta: "Referencia", max: 120, clase: "is-full" })}
            ${ui.campo({ nombre: "observacion", etiqueta: "Observación", tipo: "textarea", max: 300, clase: "is-full" })}`,
        async alGuardar(form) {
            ui.exigir(form, [
                ["monto", "El monto debe ser mayor que cero.", valor => Number(valor) > 0],
                ["monto", "El monto excede el pendiente.", valor => Number(valor) <= pendiente + 0.001],
                ["monto", "El monto admite hasta 2 decimales.", valor => /^\d+(\.\d{1,2})?$/.test(valor)]
            ]);
            const datos = ui.valores(form);
            await PagoSuscripcionService.registrar({ pagoSuscripcionId: cargo.pagoSuscripcionId, ...datos });
            AppNotifications.success("Pago registrado correctamente");
            await cargar();
        }
    });
}

async function ver(cargo) {
    const { raiz: modal } = ui.modal({
        titulo: "Detalle del cargo",
        subtitulo: `${cargo.clienteNombre} · ${cargo.planNombre}`,
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
    raiz.querySelector("[data-estado]").value = estado;
    raiz.querySelector("[data-buscar]").addEventListener("input", event => {
        window.clearTimeout(temporizador);
        temporizador = window.setTimeout(() => {
            buscar = event.target.value.trim();
            pagina = 1;
            cargar();
        }, 350);
    });
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
        const boton = event.target.closest("[data-action]");
        if (!boton) return;
        const cargo = registros.find(p => String(p.pagoSuscripcionId) === boton.closest("[data-row]")?.dataset.row);
        if (!cargo) return;
        if (boton.dataset.action === "pagar") pagar(cargo);
        if (boton.dataset.action === "ver") ver(cargo);
    });
    await cargar();
}

export function reset() {
    pagina = 1;
    buscar = "";
    window.clearTimeout(temporizador);
}
