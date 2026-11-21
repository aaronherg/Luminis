let ui;
let raiz;
let cliente = null;

export function render() {
    return `
        <div class="fr-grid-2">
            <section class="fr-panel">
                <header class="fr-panel-head"><div><h2>Datos de la cuenta</h2><p>Información de tu empresa o perfil personal</p></div></header>
                <div class="fr-panel-body">
                    <form class="fr-form" data-perfil novalidate></form>
                </div>
            </section>
            <section class="fr-panel">
                <header class="fr-panel-head"><div><h2>Mi usuario</h2><p>Datos de tu sesión</p></div></header>
                <div class="fr-panel-body" data-usuario></div>
            </section>
        </div>`;
}

function pintarFormulario() {
    const editable = ui.puede("perfil", "modificar");
    const atributos = editable ? "" : "disabled";
    const c = cliente || {};
    raiz.querySelector("[data-perfil]").innerHTML = `
        ${ui.campo({ nombre: "tipoCliente", etiqueta: "Tipo", tipo: "select", valor: c.tipoCliente, atributos, opciones: [{ valor: "Empresa", texto: "Empresa" }, { valor: "Persona", texto: "Persona" }] })}
        ${ui.campo({ nombre: "documento", etiqueta: "Documento (RNC o cédula)", valor: c.documento, max: 30, atributos })}
        ${ui.campo({ nombre: "nombreComercial", etiqueta: "Nombre comercial", valor: c.nombreComercial, requerido: true, max: 150, atributos })}
        ${ui.campo({ nombre: "razonSocial", etiqueta: "Razón social", valor: c.razonSocial, max: 150, atributos })}
        ${ui.campo({ nombre: "correo", etiqueta: "Correo", tipo: "email", valor: c.correo, requerido: true, max: 150, atributos })}
        <div class="fr-field"><span>Teléfono</span><div class="telefono-inline"><input class="fr-input" name="prefijoTelefono" data-phone-prefix value="${ui.escapar(c.prefijoTelefono || "+1")}" aria-label="Prefijo" ${atributos}><input class="fr-input" name="telefono" value="${ui.escapar(c.telefono || "")}" maxlength="20" aria-label="Teléfono" ${atributos}></div></div>
        ${ui.campo({ nombre: "direccion", etiqueta: "Dirección", valor: c.direccion, max: 250, clase: "is-full", atributos })}
        ${editable ? '<div class="is-full fr-form-actions"><button class="fr-btn fr-btn-primary" type="submit">Guardar cambios</button></div>' : ""}`;
}

function pintarUsuario() {
    const sesion = ui.sesion;
    const perfil = sesion.perfil || {};
    raiz.querySelector("[data-usuario]").innerHTML = `
        <dl class="fr-kv">
            <div><dt>Usuario</dt><dd>@${ui.escapar(sesion.usuario)}</dd></div>
            <div><dt>Nombre</dt><dd>${ui.escapar([perfil.nombres, perfil.apellidos].filter(Boolean).join(" ") || "—")}</dd></div>
            <div><dt>Correo</dt><dd>${ui.escapar(perfil.correo || "—")}</dd></div>
            <div><dt>Rol</dt><dd>${perfil.propietario ? "Propietario" : "Usuario"}</dd></div>
        </dl>
        <div class="fr-form-actions" style="margin-top:18px"><button class="fr-btn" type="button" data-clave>${ui.icono("lock")}<span>Cambiar contraseña</span></button></div>`;
    raiz.querySelector("[data-clave]").addEventListener("click", () => document.querySelector("[data-change-password]")?.click());
}

async function cargar() {
    try {
        cliente = await ClienteService.actual();
        pintarFormulario();
        pintarUsuario();
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const form = raiz.querySelector("[data-perfil]");
    form.addEventListener("submit", async event => {
        event.preventDefault();
        const boton = form.querySelector('button[type="submit"]');
        await ui.ocupado(boton, async () => {
            try {
                ui.exigir(form, [
                    ["nombreComercial", "El nombre comercial es obligatorio."],
                    ["correo", "Ingresa un correo válido.", valor => ui.correoValido(valor)]
                ]);
                await ClienteService.actualizarActual({ ...ui.valores(form), clienteUuid: cliente.clienteUuid, diaCorte: cliente.diaCorte, estado: cliente.estado });
                AppNotifications.success("Datos actualizados correctamente");
                await cargar();
            } catch (error) {
                ui.notificarError(error);
            }
        });
    });
    await cargar();
}

export function reset() {
    cliente = null;
}
