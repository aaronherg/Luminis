let ui;
let raiz;
let lista = [];

export function render() {
    return `
        <div class="fr-toolbar">
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn fr-btn-primary" type="button" data-nuevo hidden>${LuminisPanel.icono("plus")}<span>Nueva API key</span></button>
        </div>
        <section class="fr-panel">
            <p style="margin:0 0 14px;opacity:.75;font-size:.86rem">Llaves de administración para integraciones. Se envían en el encabezado <span class="fr-mono">X-Admin-Key</span> y actúan con los permisos de tu rol.</p>
            <div data-lista></div>
        </section>`;
}

function fecha(f) {
    return f ? new Date(f).toLocaleString(document.documentElement.lang === "en" ? "en-US" : "es-DO", { dateStyle: "medium", timeStyle: "short" }) : "Nunca";
}

async function cargar() {
    try {
        lista = (await ApiKeyAdministracionService.listar()) || [];
        const editar = ui.puede("apikeysadmin", "editar");
        const revocar = ui.puede("apikeysadmin", "revocar");
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: lista,
            clave: "uuid",
            vacio: "Aún no has creado API keys. Crea una para conectar integraciones.",
            columnas: [
                { titulo: "Nombre", valor: x => `<span class="fr-cell-main">${ui.escapar(x.nombre)}</span>` },
                { titulo: "Llave", valor: x => `<span class="fr-mono">${ui.escapar(x.prefijo || "")}…</span>` },
                { titulo: "Último uso", valor: x => x.fechaUltimoUso ? ui.escapar(fecha(x.fechaUltimoUso)) : "Nunca" },
                { titulo: "Estado", valor: x => ui.estado(x.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: x => x.estado === "Activa"
                        ? ((editar ? ui.botonAccion("editar", "pencil", "Renombrar") : "") + (revocar ? ui.botonAccion("revocar", "ban", "Revocar", "is-danger") : ""))
                        : ""
                }
            ]
        });
    } catch (error) {
        ui.notificarError(error);
    }
}

function formulario(llave) {
    const nueva = !llave;
    ui.formulario({
        titulo: nueva ? "Nueva API key" : "Renombrar API key",
        ancho: "sm",
        campos: ui.campo({ nombre: "nombre", etiqueta: "Nombre", valor: llave?.nombre, requerido: true, max: 120, clase: "is-full", ayuda: "Para reconocer dónde la usarás." }),
        async alGuardar(form) {
            ui.exigir(form, [["nombre", "El nombre es obligatorio."]]);
            const nombre = ui.valores(form).nombre;
            if (nueva) {
                const creada = await ApiKeyAdministracionService.crear(nombre);
                mostrarClave(creada);
            } else {
                await ApiKeyAdministracionService.renombrar(llave.uuid, nombre);
                AppNotifications.success("API key actualizada correctamente");
            }
            await cargar();
        }
    });
}

function mostrarClave(creada) {
    const { raiz: m } = ui.modal({
        titulo: "Tu nueva API key",
        ancho: "sm",
        cuerpo: `<p style="margin:0 0 12px;opacity:.8;font-size:.88rem">Cópiala ahora: por seguridad no volveremos a mostrarla. Envíala en el encabezado <span class="fr-mono">X-Admin-Key</span>.</p>
            <div style="display:flex;gap:8px;align-items:center"><code class="fr-mono" style="flex:1;min-width:0;overflow:auto;padding:12px 14px;border-radius:10px;background:rgba(128,128,128,.14)">${ui.escapar(creada.clave)}</code>
            <button class="fr-btn" type="button" data-copiar>${LuminisPanel.icono("copy")}<span>Copiar</span></button></div>`
    });
    m.querySelector("[data-copiar]").addEventListener("click", () => ui.copiar(creada.clave));
}

async function revocar(llave) {
    const ok = await ui.confirmar({
        titulo: "Revocar API key",
        mensaje: `Las integraciones que usen «${llave.nombre}» dejarán de funcionar de inmediato. No se puede deshacer.`,
        textoConfirmar: "Revocar",
        peligro: true
    });
    if (!ok) return;
    try {
        await ApiKeyAdministracionService.revocar(llave.uuid);
        AppNotifications.success("API key revocada correctamente");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const nuevo = raiz.querySelector("[data-nuevo]");
    nuevo.hidden = !ui.puede("apikeysadmin", "crear");
    nuevo.addEventListener("click", () => formulario(null));
    raiz.querySelector("[data-lista]").addEventListener("click", event => {
        const boton = event.target.closest("[data-action]");
        if (!boton) return;
        const llave = lista.find(x => String(x.uuid) === boton.closest("[data-row]")?.dataset.row);
        if (!llave) return;
        if (boton.dataset.action === "editar") formulario(llave);
        if (boton.dataset.action === "revocar") revocar(llave);
    });
    await cargar();
}

export function reset() {
    lista = [];
}
