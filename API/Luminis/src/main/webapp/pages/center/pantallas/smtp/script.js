let ui;
let raiz;
let cfg = null;

export function render() {
    return `<div class="fr-grid-2">
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Servidor SMTP</h2><p>Correo saliente para notificaciones y códigos de verificación.</p></div></header>
            <div class="fr-panel-body"><form class="fr-form" data-smtp novalidate></form></div>
        </section>
        <section class="fr-panel">
            <header class="fr-panel-head"><div><h2>Probar envío</h2><p>Envía un correo de prueba con la configuración guardada.</p></div></header>
            <div class="fr-panel-body" data-prueba></div>
        </section>
    </div>`;
}

function pintar() {
    const c = cfg || {};
    const editable = ui.puede("smtp", "editar");
    const at = editable ? "" : "disabled";
    raiz.querySelector("[data-smtp]").innerHTML = `
        ${ui.campo({ nombre: "habilitado", etiqueta: "Activar el servicio SMTP", tipo: "checkbox", valor: c.smtpHabilitado, clase: "is-full", atributos: at })}
        ${ui.campo({ nombre: "host", etiqueta: "Servidor (host)", valor: c.smtpHost, max: 160, requerido: true, atributos: at })}
        ${ui.campo({ nombre: "puerto", etiqueta: "Puerto", valor: c.smtpPuerto || "587", max: 6, atributos: `inputmode="numeric" ${at}` })}
        ${ui.campo({ nombre: "seguridad", etiqueta: "Seguridad", tipo: "select", valor: c.smtpSeguridad || "STARTTLS", atributos: at, opciones: [{ valor: "STARTTLS", texto: "STARTTLS" }, { valor: "SSL", texto: "SSL/TLS" }, { valor: "NONE", texto: "Ninguna" }] })}
        ${ui.campo({ nombre: "usuario", etiqueta: "Usuario", valor: c.smtpUsuario, max: 160, requerido: true, atributos: at })}
        ${ui.campo({ nombre: "contrasena", etiqueta: "Contraseña", tipo: "password", max: 200, ayuda: c.smtpTieneContrasena ? "Hay una contraseña guardada. Déjala vacía para conservarla." : "", atributos: `autocomplete="new-password" ${at}` })}
        ${ui.campo({ nombre: "remitente", etiqueta: "Correo remitente", tipo: "email", valor: c.smtpRemitente, max: 160, atributos: at })}
        ${ui.campo({ nombre: "nombreRemitente", etiqueta: "Nombre del remitente", valor: c.smtpNombreRemitente, max: 120, atributos: at })}
        ${editable ? '<div class="is-full fr-form-actions"><button class="fr-btn fr-btn-primary" type="submit">Guardar cambios</button></div>' : ""}`;
    raiz.querySelector("[data-prueba]").innerHTML = `
        <form class="fr-form" data-form-prueba novalidate>
            ${ui.campo({ nombre: "destino", etiqueta: "Correo de destino", tipo: "email", max: 160, ayuda: "Si lo dejas vacío, se usará el correo remitente.", clase: "is-full" })}
            <div class="is-full fr-form-actions"><button class="fr-btn" type="submit" ${ui.puede("smtp", "probar") ? "" : "disabled"}>${ui.icono("send")}<span>Enviar correo de prueba</span></button></div>
        </form>`;
}

async function cargar() {
    try {
        cfg = await LuminisApi.post("/api/administracion/notificaciones/smtp/get", {});
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
        if (form.matches("[data-smtp]")) {
            await ui.ocupado(boton, async () => {
                try {
                    ui.exigir(form, [["host", "El servidor es obligatorio."], ["usuario", "El usuario es obligatorio."]]);
                    cfg = await LuminisApi.put("/api/administracion/notificaciones/smtp/save", ui.valores(form));
                    AppNotifications.success("Servicio SMTP guardado correctamente");
                    pintar();
                } catch (error) { ui.notificarError(error); }
            });
        } else if (form.matches("[data-form-prueba]")) {
            await ui.ocupado(boton, async () => {
                try {
                    await LuminisApi.post("/api/administracion/notificaciones/smtp/probar", ui.valores(form));
                    AppNotifications.success("Correo de prueba enviado correctamente");
                } catch (error) { ui.notificarError(error); }
            });
        }
    });
    await cargar();
}

export function reset() {
    cfg = null;
}
