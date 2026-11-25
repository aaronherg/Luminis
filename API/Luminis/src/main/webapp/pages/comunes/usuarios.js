export function crearPantallaUsuarios(config) {
    let ui;
    let raiz;
    let usuarios = [];
    let roles = [];
    let filtro = "";

    const render = () => `
        <div class="fr-toolbar">
            <label class="fr-search">${LuminisPanel.icono("search")}<input class="fr-input" type="search" placeholder="Buscar por nombre, usuario o correo" data-filtro aria-label="Buscar usuarios"></label>
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn" type="button" data-recargar>${LuminisPanel.icono("refresh")}<span>Actualizar</span></button>
            <button class="fr-btn fr-btn-primary" type="button" data-nuevo hidden>${LuminisPanel.icono("plus")}<span>Nuevo usuario</span></button>
        </div>
        <section class="fr-panel"><div data-lista></div></section>`;

    function nombreRoles(usuario) {
        const ids = new Set(usuario.rolesIds || []);
        const nombres = roles.filter(r => ids.has(r[config.rolId])).map(r => r.nombre);
        return nombres.length ? nombres.map(n => `<span class="fr-badge is-plain">${ui.escapar(n)}</span>`).join(" ") : '<span class="fr-cell-sub">Sin rol</span>';
    }

    function pintar() {
        const texto = filtro.toLowerCase();
        const filas = usuarios.filter(u =>
            [u.nombres, u.apellidos, u.usuario, u.correo].filter(Boolean).join(" ").toLowerCase().includes(texto)
        );
        const puedeModificar = ui.puede("usuarios", "modificar");
        const puedeEstado = ui.puede("usuarios", "cambiar_estado");
        const puedeClave = ui.puede("usuarios", config.accionContrasena);
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas,
            clave: config.id,
            vacio: "No hay usuarios registrados.",
            columnas: [
                {
                    titulo: "Usuario",
                    valor: u => `<span class="fr-cell-main">${ui.escapar([u.nombres, u.apellidos].filter(Boolean).join(" "))}${u.propietario ? ' <span class="fr-badge is-info is-plain">Propietario</span>' : ""}</span><span class="fr-cell-sub">@${ui.escapar(u.usuario)}</span>`
                },
                { titulo: "Correo", valor: u => ui.escapar(u.correo || "—") },
                { titulo: "Roles", valor: nombreRoles },
                { titulo: "Estado", valor: u => ui.estado(u.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: u =>
                        [
                            puedeModificar ? ui.botonAccion("editar", "pencil", "Editar") : "",
                            puedeClave ? ui.botonAccion("clave", "lock", "Restablecer contraseña") : "",
                            puedeEstado && !u.propietario
                                ? u.estado === "Activo"
                                    ? ui.botonAccion("suspender", "ban", "Suspender", "is-danger")
                                    : ui.botonAccion("activar", "play", "Reactivar")
                                : "",
                            puedeEstado && !u.propietario ? ui.botonAccion("eliminar", "trash", "Eliminar", "is-danger") : ""
                        ].join("")
                }
            ]
        });
    }

    async function cargar() {
        try {
            [usuarios, roles] = await Promise.all([config.servicio.listar(config.clienteId?.()), config.rolesServicio()]);
            pintar();
        } catch (error) {
            ui.notificarError(error);
        }
    }

    function rolesCampo(seleccionados = [], bloqueado = false) {
        const ids = new Set(seleccionados);
        if (!roles.length) return '<p class="fr-note">Aún no hay roles. Crea uno en el módulo Roles.</p>';
        return `<fieldset><legend>Roles</legend><div class="fr-chip-list">${roles
            .map(
                r =>
                    `<label class="fr-check"><input type="checkbox" name="rolesIds" data-multiple value="${r[config.rolId]}" ${ids.has(r[config.rolId]) ? "checked" : ""} ${bloqueado ? "disabled" : ""}><span>${ui.escapar(r.nombre)}</span></label>`
            )
            .join("")}</div></fieldset>`;
    }

    function abrirFormulario(usuario) {
        const nuevo = !usuario;
        ui.formulario({
            titulo: nuevo ? "Nuevo usuario" : "Editar usuario",
            subtitulo: nuevo ? "El usuario podrá iniciar sesión con las acciones de sus roles." : `@${usuario.usuario}`,
            ancho: "md",
            campos: `
                ${ui.campo({ nombre: "nombres", etiqueta: "Nombres", valor: usuario?.nombres, requerido: true, max: 100 })}
                ${ui.campo({ nombre: "apellidos", etiqueta: "Apellidos", valor: usuario?.apellidos, max: 100 })}
                ${ui.campo({ nombre: "correo", etiqueta: "Correo", tipo: "email", valor: usuario?.correo, requerido: true, max: 150 })}
                <div class="fr-field"><span>Teléfono</span><div class="telefono-inline"><input class="fr-input" name="prefijoTelefono" data-phone-prefix value="${ui.escapar(usuario?.prefijoTelefono || "+1")}" aria-label="Prefijo"><input class="fr-input" name="telefono" value="${ui.escapar(usuario?.telefono || "")}" maxlength="20" aria-label="Teléfono"></div></div>
                ${nuevo ? ui.campo({ nombre: "usuario", etiqueta: "Usuario", requerido: true, max: 60, ayuda: "De 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @", atributos: 'autocomplete="off"' }) : ""}
                ${nuevo ? ui.campo({ nombre: "contrasena", etiqueta: "Contraseña", tipo: "password", requerido: true, ayuda: ui.MENSAJE_CONTRASENA, atributos: 'autocomplete="new-password"' }) : ""}
                ${rolesCampo(usuario?.rolesIds || [], usuario?.propietario)}
                ${usuario?.propietario ? '<p class="fr-note">Los roles del propietario no se pueden cambiar.</p>' : ""}`,
            async alGuardar(form) {
                const reglas = [
                    ["nombres", "Los nombres son obligatorios."],
                    ["correo", "Ingresa un correo válido.", valor => ui.correoValido(valor)]
                ];
                if (nuevo) {
                    reglas.push(["usuario", "El usuario debe tener de 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @", valor => ui.usuarioValido(valor)]);
                    reglas.push(["contrasena", ui.MENSAJE_CONTRASENA, valor => ui.contrasenaValida(valor)]);
                }
                ui.exigir(form, reglas);
                const datos = ui.valores(form);
                datos.rolesIds = usuario?.propietario ? usuario.rolesIds : datos.rolesIds || [];
                if (config.clienteId?.()) datos.clienteId = config.clienteId();
                if (nuevo) {
                    await config.servicio.agregar(datos);
                    AppNotifications.success("Usuario creado correctamente");
                } else {
                    await config.servicio.actualizar({ ...datos, usuario: usuario.usuario, [config.uuid]: usuario[config.uuid] });
                    AppNotifications.success("Usuario actualizado correctamente");
                }
                await cargar();
            }
        });
    }

    function restablecer(usuario) {
        ui.formulario({
            titulo: "Restablecer contraseña",
            subtitulo: `@${usuario.usuario}`,
            ancho: "sm",
            textoGuardar: "Restablecer",
            campos: `${ui.campo({ nombre: "contrasena", etiqueta: "Nueva contraseña", tipo: "password", requerido: true, clase: "is-full", ayuda: ui.MENSAJE_CONTRASENA, atributos: 'autocomplete="new-password"' })}
                <p class="fr-note">El usuario deberá cambiarla en su próximo inicio de sesión.</p>`,
            async alGuardar(form) {
                ui.exigir(form, [["contrasena", ui.MENSAJE_CONTRASENA, valor => ui.contrasenaValida(valor)]]);
                await config.servicio.restablecerContrasena(usuario[config.id], form.elements.contrasena.value);
                AppNotifications.success("Contraseña restablecida correctamente");
            }
        });
    }

    async function cambiarEstado(usuario, estado) {
        const suspender = estado === "Suspendido";
        const ok = await ui.confirmar({
            titulo: suspender ? "Suspender usuario" : "Reactivar usuario",
            mensaje: suspender
                ? `@${usuario.usuario} no podrá iniciar sesión hasta que lo reactives.`
                : `@${usuario.usuario} podrá volver a iniciar sesión.`,
            textoConfirmar: suspender ? "Suspender" : "Reactivar",
            peligro: suspender
        });
        if (!ok) return;
        try {
            await config.servicio.actualizar({
                [config.uuid]: usuario[config.uuid],
                usuario: usuario.usuario,
                nombres: usuario.nombres,
                apellidos: usuario.apellidos,
                correo: usuario.correo,
                prefijoTelefono: usuario.prefijoTelefono,
                telefono: usuario.telefono,
                rolesIds: usuario.rolesIds,
                clienteId: usuario.clienteId,
                estado
            });
            AppNotifications.success(suspender ? "Usuario suspendido" : "Usuario reactivado");
            await cargar();
        } catch (error) {
            ui.notificarError(error);
        }
    }

    async function eliminar(usuario) {
        const ok = await ui.confirmar({
            titulo: "Eliminar usuario",
            mensaje: `Se eliminará @${usuario.usuario}. Esta acción no se puede deshacer.`,
            textoConfirmar: "Eliminar",
            peligro: true
        });
        if (!ok) return;
        try {
            await config.servicio.eliminar(usuario[config.id]);
            AppNotifications.success("Usuario eliminado correctamente");
            await cargar();
        } catch (error) {
            ui.notificarError(error);
        }
    }

    async function bind(stage, api) {
        ui = api;
        raiz = stage;
        raiz.querySelector("[data-nuevo]").hidden = !ui.puede("usuarios", "crear");
        raiz.querySelector("[data-nuevo]").addEventListener("click", () => abrirFormulario(null));
        raiz.querySelector("[data-recargar]").addEventListener("click", cargar);
        raiz.querySelector("[data-filtro]").addEventListener("input", event => {
            filtro = event.target.value.trim();
            pintar();
        });
        raiz.querySelector("[data-lista]").addEventListener("click", event => {
            const boton = event.target.closest("[data-action]");
            if (!boton) return;
            const id = boton.closest("[data-row]")?.dataset.row;
            const usuario = usuarios.find(u => String(u[config.id]) === id);
            if (!usuario) return;
            const acciones = {
                editar: () => abrirFormulario(usuario),
                clave: () => restablecer(usuario),
                suspender: () => cambiarEstado(usuario, "Suspendido"),
                activar: () => cambiarEstado(usuario, "Activo"),
                eliminar: () => eliminar(usuario)
            };
            acciones[boton.dataset.action]?.();
        });
        await cargar();
    }

    const skeleton = () => window.LuminisSk.lista({ filtros: 1, columnas: 5 });
    return { render, bind, skeleton, reset: () => (filtro = "") };
}
