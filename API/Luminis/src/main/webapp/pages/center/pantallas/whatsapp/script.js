let ui;
let raiz;
let cfg = null;

export function render() {
    return `<div class="fr-grid-2">
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Servicio de WhatsApp</h2><p>Mensajería por WhatsApp para avisos y notificaciones.</p></div></header>
            <div class="fr-panel-body"><form class="fr-form" data-whatsapp novalidate></form></div>
        </section>
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Probar envío</h2><p>Envía un mensaje de prueba con la configuración guardada.</p></div></header>
            <div class="fr-panel-body" data-prueba></div>
        </section>
    </div>`;
}

function pintar() {
    const c = cfg || {};
    const editable = ui.puede("whatsapp", "editar");
    const at = editable ? "" : "disabled";
    raiz.querySelector("[data-whatsapp]").innerHTML = `
        <label class="fr-switch-row is-full"><span><b>Activar el servicio de WhatsApp</b><small>Úsalo para enviar avisos y mensajes por WhatsApp.</small></span><label class="fr-switch"><input type="checkbox" name="habilitado" ${c.whatsappHabilitado ? "checked" : ""} ${at}><span></span></label></label>
        ${ui.campo({ nombre: "dispositivo", etiqueta: "Dispositivo", valor: c.whatsappDispositivo, max: 160, requerido: true, atributos: at })}
        ${ui.campo({ nombre: "apiKey", etiqueta: "API key", tipo: "password", max: 200, ayuda: c.whatsappTieneApiKey ? "Hay una API key guardada. Déjala vacía para conservarla." : "", atributos: `autocomplete="new-password" ${at}` })}
        ${ui.campo({ nombre: "telefonoPrueba", etiqueta: "Teléfono de prueba", valor: c.whatsappTelefonoPrueba, max: 30, atributos: at })}
        ${editable ? '<div class="is-full fr-form-actions"><button class="fr-btn fr-btn-primary" type="submit">Guardar cambios</button></div>' : ""}`;
    raiz.querySelector("[data-prueba]").innerHTML = `
        <form class="fr-form" data-form-prueba novalidate>
            ${ui.campo({ nombre: "destino", etiqueta: "Teléfono de destino", max: 30, ayuda: "Si lo dejas vacío, se usará el teléfono de prueba.", clase: "is-full" })}
            <div class="is-full fr-form-actions"><button class="fr-btn" type="submit" ${ui.puede("whatsapp", "probar") ? "" : "disabled"}>${ui.icono("send")}<span>Enviar mensaje de prueba</span></button></div>
        </form>`;
}

async function cargar() {
    try {
        cfg = await LuminisApi.post("/api/administracion/notificaciones/whatsapp/get", {});
        pintar();
    } catch (error) { ui.notificarError(error); }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    raiz.addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.target;
        const boton = form.querySelector('button[type="submit"]');
        if (form.matches("[data-whatsapp]")) {
            await ui.ocupado(boton, async () => {
                try {
                    ui.exigir(form, [["dispositivo", "El dispositivo es obligatorio."]]);
                    cfg = await LuminisApi.put("/api/administracion/notificaciones/whatsapp/save", ui.valores(form));
                    AppNotifications.success("Servicio de WhatsApp guardado correctamente");
                    pintar();
                } catch (error) { ui.notificarError(error); }
            });
        } else if (form.matches("[data-form-prueba]")) {
            await ui.ocupado(boton, async () => {
                try {
                    await LuminisApi.post("/api/administracion/notificaciones/whatsapp/probar", ui.valores(form));
                    AppNotifications.success("Mensaje de prueba enviado correctamente");
                } catch (error) { ui.notificarError(error); }
            });
        }
    });
    await cargar();
}

export function skeleton() {
    const b = (css) => `<div class="fr-skeleton" style="${css}"></div>`;
    const campo = `<div style="display:grid;gap:7px">${b("height:12px;width:130px")}${b("height:44px;border-radius:12px")}</div>`;
    const panel = (cuerpo) => `<section class="fr-panel"><div style="padding:18px 20px;border-bottom:1px solid var(--line);display:grid;gap:8px">${b("height:16px;width:190px")}${b("height:11px;width:250px")}</div><div style="padding:20px;display:grid;gap:14px">${cuerpo}</div></section>`;
    return `<div class="fr-grid-2">
        ${panel(`${b("height:56px;border-radius:14px")}${campo}${campo}${campo}${b("height:42px;width:160px;border-radius:12px;justify-self:end")}`)}
        ${panel(`${campo}${b("height:42px;width:200px;border-radius:12px")}`)}
    </div>`;
}

export function reset() {
    cfg = null;
}
