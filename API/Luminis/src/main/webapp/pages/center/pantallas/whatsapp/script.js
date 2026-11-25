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
        ${ui.campo({ nombre: "habilitado", etiqueta: "Activar el servicio de WhatsApp", tipo: "checkbox", valor: c.whatsappHabilitado, clase: "is-full", atributos: at })}
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

export function reset() {
    cfg = null;
}
