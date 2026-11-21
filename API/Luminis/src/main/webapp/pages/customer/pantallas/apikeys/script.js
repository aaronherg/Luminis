let ui;
let raiz;
let llaves = [];
let permisos = [];

const NOMBRES = { conectar: "Conectar al socket", enviar: "Enviar mensajes por socket", publicar: "Publicar por API REST", consultar_presencia: "Consultar presencia", desconectar: "Desconectar usuarios", consultar_cuenta: "Consultar cuenta y consumo" };

export function render() {
    return `
        <div class="fr-toolbar">
            <p class="fr-toolbar-text">Envía tu API key en el encabezado <code class="fr-mono">X-Api-Key</code>. Conéctate a <code class="fr-mono">/socket?apikey=…&amp;usuario=…</code> o usa <code class="fr-mono">/api/luminis/…</code>.</p>
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn fr-btn-primary" type="button" data-nueva hidden>${LuminisPanel.icono("plus")}<span>Nueva API key</span></button>
        </div>
        <section class="fr-panel"><div data-lista></div></section>`;
}

function chips(valor) {
    return String(valor || "")
        .split(",")
        .filter(Boolean)
        .map(p => `<span class="fr-badge is-plain">${ui.escapar(NOMBRES[p] || p)}</span>`)
        .join(" ");
}

async function cargar() {
    try {
        llaves = (await ApiKeyService.listar()) || [];
        const crear = ui.puede("apikeys", "crear");
        const revocar = ui.puede("apikeys", "revocar");
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: llaves,
            clave: "apiKeyId",
            vacio: "Aún no tienes API keys. Crea una para conectar tu aplicación.",
            columnas: [
                { titulo: "Nombre", valor: k => `<span class="fr-cell-main" data-i18n-skip>${ui.escapar(k.nombre)}</span><span class="fr-cell-sub fr-mono">${ui.escapar(k.prefijo)}••••••••</span>` },
                { titulo: "Permisos", valor: k => `<div class="fr-chip-list">${chips(k.permisos)}</div>` },
                { titulo: "Último uso", valor: k => ui.formato.fechaHora(k.fechaUltimoUso) },
                { titulo: "Estado", valor: k => ui.estado(k.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: () => (crear ? ui.botonAccion("editar", "pencil", "Editar") : "") + (revocar ? ui.botonAccion("revocar", "trash", "Revocar", "is-danger") : "")
                }
            ]
        });
    } catch (error) {
        ui.notificarError(error);
    }
}

function camposPermisos(seleccionados) {
    const activos = new Set(String(seleccionados || "").split(",").filter(Boolean));
    return `<fieldset><legend>Permisos</legend><div class="fr-permiso-acciones">${permisos
        .map(
            p =>
                `<label class="fr-check"><input type="checkbox" name="permisos" data-multiple value="${ui.escapar(p)}" ${activos.has(p) ? "checked" : ""}><span>${ui.escapar(NOMBRES[p] || p)}</span></label>`
        )
        .join("")}</div></fieldset>`;
}

function mostrarClave(llave) {
    const { raiz: modal } = ui.modal({
        titulo: "Tu nueva API key",
        subtitulo: llave.nombre,
        ancho: "md",
        cuerpo: `
            <p class="fr-note is-accent">Cópiala ahora y guárdala en un lugar seguro. Por seguridad no se volverá a mostrar.</p>
            <div class="fr-secret" data-i18n-skip><code>${ui.escapar(llave.clave)}</code><button class="fr-btn fr-btn-sm" type="button" data-copiar>${ui.icono("copy")}<span>Copiar</span></button></div>`,
        pie: '<button class="fr-btn fr-btn-primary" type="button" data-modal-close>Ya la guardé</button>'
    });
    modal.querySelector("[data-copiar]").addEventListener("click", () => ui.copiar(llave.clave));
}

function formulario(llave) {
    const nueva = !llave;
    ui.formulario({
        titulo: nueva ? "Nueva API key" : "Editar API key",
        ancho: "md",
        textoGuardar: nueva ? "Crear API key" : "Guardar",
        campos: `
            ${ui.campo({ nombre: "nombre", etiqueta: "Nombre", valor: llave?.nombre, requerido: true, max: 80, clase: "is-full", ayuda: "Por ejemplo: Producción, App móvil, Integración ERP." })}
            ${camposPermisos(llave?.permisos ?? "conectar,enviar,consultar_presencia,consultar_cuenta")}
            ${nueva ? "" : ui.campo({ nombre: "estado", etiqueta: "Estado", tipo: "select", valor: llave.estado, opciones: [{ valor: "Activo", texto: "Activa" }, { valor: "Suspendido", texto: "Suspendida" }] })}`,
        async alGuardar(form) {
            ui.exigir(form, [["nombre", "El nombre es obligatorio."]]);
            const datos = ui.valores(form);
            if (!datos.permisos?.length) throw new Error("Selecciona al menos un permiso.");
            if (nueva) {
                const creada = await ApiKeyService.agregar({ nombre: datos.nombre, permisos: datos.permisos });
                await cargar();
                window.setTimeout(() => mostrarClave(creada), 200);
            } else {
                await ApiKeyService.actualizar({ apiKeyId: llave.apiKeyId, nombre: datos.nombre, permisos: datos.permisos, estado: datos.estado });
                AppNotifications.success("API key actualizada correctamente");
                await cargar();
            }
        }
    });
}

async function revocar(llave) {
    const ok = await ui.confirmar({
        titulo: "Revocar API key",
        mensaje: `Las aplicaciones que usen ${llave.nombre} dejarán de funcionar de inmediato. Esta acción no se puede deshacer.`,
        textoConfirmar: "Revocar",
        peligro: true
    });
    if (!ok) return;
    try {
        await ApiKeyService.revocar(llave.apiKeyId);
        AppNotifications.success("API key revocada correctamente");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const nueva = raiz.querySelector("[data-nueva]");
    nueva.hidden = !ui.puede("apikeys", "crear");
    nueva.addEventListener("click", () => formulario(null));
    raiz.querySelector("[data-lista]").addEventListener("click", event => {
        const boton = event.target.closest("[data-action]");
        if (!boton) return;
        const llave = llaves.find(k => String(k.apiKeyId) === boton.closest("[data-row]")?.dataset.row);
        if (!llave) return;
        if (boton.dataset.action === "editar") formulario(llave);
        if (boton.dataset.action === "revocar") revocar(llave);
    });
    try {
        permisos = (await ApiKeyService.permisos()) || Object.keys(NOMBRES);
    } catch (_) {
        permisos = Object.keys(NOMBRES);
    }
    await cargar();
}

export function reset() {
    llaves = [];
}
