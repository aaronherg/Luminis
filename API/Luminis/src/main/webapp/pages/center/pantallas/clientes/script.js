let ui;
let raiz;
let pagina = 1;
let buscar = "";
let estadoFiltro = "";
let registros = [];
let temporizador = 0;

export function render() {
    return `
        <div class="fr-toolbar">
            <label class="fr-search">${LuminisPanel.icono("search")}<input class="fr-input" type="search" placeholder="Buscar por nombre, documento o correo" data-buscar aria-label="Buscar clientes"></label>
            <select class="fr-select" data-estado aria-label="Estado">
                <option value="">Todos los estados</option>
                <option value="Activo">Activos</option>
                <option value="Suspendido">Suspendidos</option>
            </select>
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn fr-btn-primary" type="button" data-nuevo hidden>${LuminisPanel.icono("plus")}<span>Nuevo cliente</span></button>
        </div>
        <section class="fr-panel"><div data-lista></div><div data-paginador></div></section>`;
}

function consumo(c) {
    if (!c.planNombre) return '<span class="fr-cell-sub">Sin plan vigente</span>';
    const usado = Number(c.peticionesUsadas || 0);
    if (c.peticionesMes == null) return `<span class="fr-cell-sub">${ui.formato.numero(usado)} · Ilimitado</span>`;
    const limite = Number(c.peticionesMes);
    const porcentaje = Math.min(100, (usado / Math.max(1, limite)) * 100);
    return `<div style="min-width:150px"><span class="fr-cell-sub">${ui.formato.numero(usado)} de ${ui.formato.numero(limite)} peticiones</span><div class="fr-meter ${porcentaje < 85 ? "is-ok" : "is-alert"}" style="margin-top:6px"><span style="width:${porcentaje}%"></span></div></div>`;
}

async function cargar() {
    try {
        const respuesta = await ClienteService.paginar({ page: pagina, size: 15, buscar: buscar || null, estado: estadoFiltro || null });
        registros = respuesta?.records || [];
        const puedeModificar = ui.puede("clientes", "modificar");
        const puedeEstado = ui.puede("clientes", "cambiar_estado");
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: registros,
            clave: "clienteId",
            vacio: buscar || estadoFiltro ? "No hay clientes que coincidan con la búsqueda." : "Aún no hay clientes registrados.",
            columnas: [
                {
                    titulo: "Cliente",
                    valor: c => `<span class="fr-cell-main">${ui.escapar(c.nombreComercial)}</span><span class="fr-cell-sub">${ui.escapar(c.tipoCliente)}${c.documento ? ` · ${ui.escapar(c.documento)}` : ""}</span>`
                },
                { titulo: "Contacto", valor: c => `<span class="fr-cell-main">${ui.escapar(c.correo)}</span><span class="fr-cell-sub">${ui.escapar([c.prefijoTelefono, c.telefono].filter(Boolean).join(" ") || "—")}</span>` },
                {
                    titulo: "Plan",
                    valor: c => (c.planNombre ? `<span class="fr-cell-main">${ui.escapar(c.planNombre)}</span><span class="fr-cell-sub">Hasta ${ui.formato.fecha(c.fechaFinPlan)}</span>` : '<span class="fr-badge is-warning">Sin plan</span>')
                },
                { titulo: "Consumo", valor: consumo },
                { titulo: "Estado", valor: c => ui.estado(c.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: c =>
                        [
                            ui.botonAccion("ver", "eye", "Ver detalle"),
                            puedeModificar ? ui.botonAccion("editar", "pencil", "Editar") : "",
                            puedeEstado
                                ? c.estado === "Activo"
                                    ? ui.botonAccion("suspender", "ban", "Suspender", "is-danger")
                                    : ui.botonAccion("activar", "play", "Reactivar")
                                : ""
                        ].join("")
                }
            ]
        });
        raiz.querySelector("[data-paginador]").innerHTML = ui.paginador(respuesta);
    } catch (error) {
        ui.notificarError(error);
    }
}

function camposCliente(c = {}) {
    return `
        ${ui.campo({
            nombre: "tipoCliente",
            etiqueta: "Tipo",
            tipo: "select",
            valor: c.tipoCliente || "Empresa",
            opciones: [
                { valor: "Empresa", texto: "Empresa" },
                { valor: "Persona", texto: "Persona" }
            ]
        })}
        ${ui.campo({ nombre: "documento", etiqueta: "RNC", valor: c.documento, max: 30 })}
        ${ui.campo({ nombre: "nombreComercial", etiqueta: "Nombre comercial", valor: c.nombreComercial, requerido: true, max: 150 })}
        ${ui.campo({ nombre: "razonSocial", etiqueta: "Razón social", valor: c.razonSocial, max: 150, clase: "is-solo-empresa" })}
        ${ui.campo({ nombre: "correo", etiqueta: "Correo", tipo: "email", valor: c.correo, requerido: true, max: 150 })}
        <div class="fr-field"><span>Teléfono</span><div class="telefono-inline"><input class="fr-input" name="prefijoTelefono" data-phone-prefix value="${ui.escapar(c.prefijoTelefono || "+1")}" aria-label="Prefijo"><input class="fr-input" name="telefono" value="${ui.escapar(c.telefono || "")}" maxlength="20" aria-label="Teléfono"></div></div>
        ${ui.campo({ nombre: "direccion", etiqueta: "Dirección", valor: c.direccion, max: 250 })}
        ${ui.campo({ nombre: "diaCorte", etiqueta: "Día de corte", tipo: "number", valor: c.diaCorte, ayuda: "Entre 1 y 28. Opcional.", atributos: 'min="1" max="28"' })}`;
}

function etiquetar(control, texto) {
    const nodo = control?.closest(".fr-field")?.querySelector(":scope > span")?.firstChild;
    if (nodo) nodo.nodeValue = texto;
}

function ajustarTipo(form) {
    const persona = form.elements.tipoCliente.value === "Persona";
    form.querySelectorAll(".is-solo-empresa").forEach(campo => {
        campo.hidden = persona;
        if (persona) campo.querySelectorAll("input").forEach(entrada => (entrada.value = ""));
    });
    etiquetar(form.elements.nombreComercial, persona ? "Nombre completo" : "Nombre comercial");
    etiquetar(form.elements.documento, persona ? "Cédula" : "RNC");
    form.elements.documento.placeholder = persona ? "000-0000000-0" : "000-00000-0";
    form.elements.nombreComercial.placeholder = persona ? "Nombre y apellidos" : "Nombre de la empresa";
}

function prepararTipo(form) {
    form.elements.tipoCliente.addEventListener("change", () => ajustarTipo(form));
    ajustarTipo(form);
}

const reglasCliente = [
    ["nombreComercial", "El nombre es obligatorio."],
    ["correo", "Ingresa un correo válido.", valor => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor)],
    ["diaCorte", "El día de corte debe estar entre 1 y 28.", valor => !valor || (Number(valor) >= 1 && Number(valor) <= 28)]
];

function nuevo() {
    ui.formulario({
        titulo: "Nuevo cliente",
        subtitulo: "Se crea la cuenta, su rol Propietario y el usuario propietario.",
        ancho: "lg",
        textoGuardar: "Crear cliente",
        alAbrir: prepararTipo,
        campos: `
            <fieldset class="fr-form-grupo"><legend>Datos del cliente</legend><div class="fr-form">${camposCliente()}</div></fieldset>
            <fieldset class="fr-form-grupo"><legend>Usuario propietario</legend><div class="fr-form">
                ${ui.campo({ nombre: "nombres", etiqueta: "Nombres", requerido: true, max: 100 })}
                ${ui.campo({ nombre: "apellidos", etiqueta: "Apellidos", max: 100 })}
                ${ui.campo({ nombre: "usuario", etiqueta: "Usuario", requerido: true, max: 60, atributos: 'autocomplete="off"' })}
                ${ui.campo({ nombre: "contrasena", etiqueta: "Contraseña", tipo: "password", requerido: true, ayuda: ui.MENSAJE_CONTRASENA, atributos: 'autocomplete="new-password"' })}
            </div></fieldset>`,
        async alGuardar(form) {
            ui.exigir(form, [
                ...reglasCliente,
                ["nombres", "Los nombres del propietario son obligatorios."],
                ["usuario", "El usuario debe tener de 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @", valor => ui.usuarioValido(valor)],
                ["contrasena", ui.MENSAJE_CONTRASENA, valor => ui.contrasenaValida(valor)]
            ]);
            await ClienteService.agregar(ui.valores(form));
            AppNotifications.success("Cliente creado correctamente");
            pagina = 1;
            await cargar();
        }
    });
}

function editar(cliente) {
    ui.formulario({
        titulo: "Editar cliente",
        subtitulo: cliente.nombreComercial,
        ancho: "lg",
        campos: camposCliente(cliente),
        alAbrir: prepararTipo,
        async alGuardar(form) {
            ui.exigir(form, reglasCliente);
            await ClienteService.actualizar({ ...ui.valores(form), clienteUuid: cliente.clienteUuid, estado: cliente.estado });
            AppNotifications.success("Cliente actualizado correctamente");
            await cargar();
        }
    });
}

async function cambiarEstado(cliente, estado) {
    const suspender = estado === "Suspendido";
    const ok = await ui.confirmar({
        titulo: suspender ? "Suspender cliente" : "Reactivar cliente",
        mensaje: suspender
            ? `${cliente.nombreComercial} y todos sus usuarios y API keys dejarán de tener acceso. Sus conexiones WebSocket se cerrarán.`
            : `${cliente.nombreComercial} recuperará el acceso a su área y su API.`,
        textoConfirmar: suspender ? "Suspender" : "Reactivar",
        peligro: suspender
    });
    if (!ok) return;
    try {
        await ClienteService.actualizar({ ...cliente, estado });
        AppNotifications.success(suspender ? "Cliente suspendido" : "Cliente reactivado");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

async function detalle(cliente) {
    const { raiz: modal } = ui.modal({
        titulo: cliente.nombreComercial,
        subtitulo: `${cliente.tipoCliente}${cliente.documento ? ` · ${cliente.documento}` : ""}`,
        ancho: "lg",
        cuerpo: '<div data-detalle style="min-height:220px"></div>'
    });
    try {
        const [suscripcion, usuarios, llaves] = await Promise.all([
            SuscripcionService.obtener(cliente.clienteId),
            UsuarioClienteService.listar(cliente.clienteId),
            ApiKeyService.listar(cliente.clienteId)
        ]);
        const vigente = suscripcion?.vigente;
        const usado = Number(suscripcion?.peticionesUsadas || 0);
        const limite = Number(suscripcion?.peticionesLimite || 0);
        const porcentaje = limite ? Math.min(100, (usado / limite) * 100) : 0;
        modal.querySelector("[data-detalle]").innerHTML = `
            <dl class="fr-kv">
                <div><dt>Correo</dt><dd>${ui.escapar(cliente.correo)}</dd></div>
                <div><dt>Teléfono</dt><dd>${ui.escapar([cliente.prefijoTelefono, cliente.telefono].filter(Boolean).join(" ") || "—")}</dd></div>
                <div><dt>Plan vigente</dt><dd>${vigente ? `${ui.escapar(vigente.planNombre)} · ${ui.escapar(vigente.tipoCobro)}` : "Sin plan vigente"}</dd></div>
                <div><dt>Vence</dt><dd>${vigente ? ui.formato.fecha(vigente.fechaFin) : "—"}</dd></div>
                <div><dt>Peticiones</dt><dd>${ui.formato.numero(usado)}${suscripcion?.peticionesIlimitadas ? " · Ilimitadas" : limite ? ` de ${ui.formato.numero(limite)}` : ""}</dd></div>
                <div><dt>Conexiones simultáneas</dt><dd>${suscripcion?.conexionesIlimitadas ? "Ilimitadas" : ui.formato.numero(suscripcion?.conexionesLimite)}</dd></div>
            </dl>
            ${limite && !suscripcion?.peticionesIlimitadas ? `<div class="fr-meter ${porcentaje < 85 ? "is-ok" : "is-alert"}" style="margin:16px 0 4px"><span style="width:${porcentaje}%"></span></div>` : ""}
            <h3 class="fr-subtitle">Usuarios</h3>
            ${ui.tabla({
                filas: usuarios || [],
                vacio: "Sin usuarios.",
                columnas: [
                    { titulo: "Nombre", valor: u => `${ui.escapar([u.nombres, u.apellidos].filter(Boolean).join(" "))}${u.propietario ? ' <span class="fr-badge is-info is-plain">Propietario</span>' : ""}` },
                    { titulo: "Usuario", valor: u => `@${ui.escapar(u.usuario)}` },
                    { titulo: "Estado", valor: u => ui.estado(u.estado) }
                ]
            })}
            <h3 class="fr-subtitle">API keys</h3>
            ${ui.tabla({
                filas: llaves || [],
                vacio: "Sin API keys.",
                columnas: [
                    { titulo: "Nombre", valor: k => ui.escapar(k.nombre) },
                    { titulo: "Prefijo", valor: k => `<span class="fr-mono">${ui.escapar(k.prefijo)}…</span>` },
                    { titulo: "Último uso", valor: k => ui.formato.fechaHora(k.fechaUltimoUso) },
                    { titulo: "Estado", valor: k => ui.estado(k.estado) }
                ]
            })}
            <h3 class="fr-subtitle">Historial de planes</h3>
            ${ui.tabla({
                filas: suscripcion?.historial || [],
                vacio: "Sin historial.",
                columnas: [
                    { titulo: "Plan", valor: d => ui.escapar(d.planNombre) },
                    { titulo: "Cobro", valor: d => ui.escapar(d.tipoCobro) },
                    { titulo: "Periodo", valor: d => `${ui.formato.fecha(d.fechaInicio)} – ${ui.formato.fecha(d.fechaFin)}` },
                    { titulo: "Estado", valor: d => ui.estado(d.estado) }
                ]
            })}`;
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const botonNuevo = raiz.querySelector("[data-nuevo]");
    botonNuevo.hidden = !ui.puede("clientes", "crear");
    botonNuevo.addEventListener("click", nuevo);
    raiz.querySelector("[data-buscar]").addEventListener("input", event => {
        window.clearTimeout(temporizador);
        temporizador = window.setTimeout(() => {
            buscar = event.target.value.trim();
            pagina = 1;
            cargar();
        }, 350);
    });
    raiz.querySelector("[data-estado]").addEventListener("change", event => {
        estadoFiltro = event.target.value;
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
        const cliente = registros.find(c => String(c.clienteId) === boton.closest("[data-row]")?.dataset.row);
        if (!cliente) return;
        const acciones = {
            ver: () => detalle(cliente),
            editar: () => editar(cliente),
            suspender: () => cambiarEstado(cliente, "Suspendido"),
            activar: () => cambiarEstado(cliente, "Activo")
        };
        acciones[boton.dataset.action]?.();
    });
    await cargar();
}

export function reset() {
    pagina = 1;
    buscar = "";
    estadoFiltro = "";
    window.clearTimeout(temporizador);
}

export function skeleton() {
    return window.LuminisSk.lista({ filtros: 2, columnas: 5 });
}
