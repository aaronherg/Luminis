import { editorPermisos, resumenRol } from "./permisos.js?v=20261127-6";

export function crearPantallaRoles(config) {
    let ui;
    let raiz;
    let roles = [];
    let modulos = [];

    const render = () => `
        <div class="fr-toolbar">
            <p class="fr-toolbar-text">Define los módulos, las opciones del menú y las acciones de cada rol. Los roles protegidos se mantienen con todos los permisos.</p>
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn fr-btn-primary" type="button" data-nuevo hidden>${LuminisPanel.icono("plus")}<span>Nuevo rol</span></button>
        </div>
        <section class="fr-panel"><div data-lista></div></section>`;

    function pintar() {
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: roles,
            clave: config.id,
            vacio: "No hay roles registrados.",
            columnas: [
                {
                    titulo: "Rol",
                    valor: r => `<span class="fr-cell-main"><span class="fr-color-dot" style="background:${ui.escapar(r.color || "#e3132b")}"></span>${ui.escapar(r.nombre)}${r.protegido ? ' <span class="fr-badge is-info is-plain">Protegido</span>' : ""}</span><span class="fr-cell-sub">${ui.escapar(r.descripcion || "")}</span>`
                },
                { titulo: "Permisos", valor: r => resumenRol(r).map(([k, v]) => `${v} ${k.toLowerCase()}`).join(" · ") },
                { titulo: "Estado", valor: r => ui.estado(r.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: r =>
                        [
                            ui.puede("roles", "modificar") ? ui.botonAccion("editar", r.protegido ? "eye" : "pencil", r.protegido ? "Ver permisos" : "Editar") : "",
                            ui.puede("roles", "eliminar") && !r.protegido ? ui.botonAccion("eliminar", "trash", "Eliminar", "is-danger") : ""
                        ].join("")
                }
            ]
        });
    }

    async function cargar() {
        try {
            [roles, modulos] = await Promise.all([config.servicio.listar(config.clienteId?.()), config.modulos()]);
            pintar();
        } catch (error) {
            ui.notificarError(error);
        }
    }

    function abrirFormulario(rol) {
        const nuevo = !rol;
        const bloqueado = Boolean(rol?.protegido);
        let editor = null;
        ui.formulario({
            titulo: nuevo ? "Nuevo rol" : bloqueado ? "Rol protegido" : "Editar rol",
            subtitulo: bloqueado ? "Este rol siempre conserva todos los permisos. Solo puedes cambiar su descripción y color." : "",
            ancho: "xl",
            campos: `
                ${ui.campo({ nombre: "nombre", etiqueta: "Nombre", valor: rol?.nombre, requerido: true, max: 80, atributos: bloqueado ? "readonly" : "" })}
                <div class="fr-field"><span>Color</span><input class="fr-input fr-color-input" type="color" name="color" value="${ui.escapar(rol?.color || "#e3132b")}"></div>
                ${ui.campo({ nombre: "descripcion", etiqueta: "Descripción", tipo: "textarea", valor: rol?.descripcion, max: 250, clase: "is-full" })}
                <div class="is-full" data-editor-permisos></div>`,
            alAbrir(form) {
                editor = editorPermisos(form.querySelector("[data-editor-permisos]"), { arbol: modulos, rol: rol || {}, protegido: bloqueado, icono: LuminisPanel.icono });
            },
            async alGuardar(form) {
                ui.exigir(form, [["nombre", "El nombre del rol es obligatorio."]]);
                const datos = ui.valores(form);
                Object.assign(datos, bloqueado
                    ? { modulosIds: rol.modulosIds || [], categoriasIds: rol.categoriasIds || [], opcionesIds: rol.opcionesIds || [], accionesIds: rol.accionesIds || [] }
                    : editor.valor());
                if (config.clienteId?.()) datos.clienteId = config.clienteId();
                if (nuevo) {
                    await config.servicio.agregar(datos);
                    AppNotifications.success("Rol creado correctamente");
                } else {
                    await config.servicio.actualizar({ ...datos, nombre: bloqueado ? rol.nombre : datos.nombre, [config.uuid]: rol[config.uuid], estado: rol.estado });
                    AppNotifications.success("Rol actualizado correctamente");
                }
                await cargar();
            }
        });
    }

    async function eliminar(rol) {
        const ok = await ui.confirmar({
            titulo: "Eliminar rol",
            mensaje: `Se eliminará el rol ${rol.nombre}. Los usuarios que lo tengan perderán sus permisos.`,
            textoConfirmar: "Eliminar",
            peligro: true
        });
        if (!ok) return;
        try {
            await config.servicio.eliminar(rol[config.id]);
            AppNotifications.success("Rol eliminado correctamente");
            await cargar();
        } catch (error) {
            ui.notificarError(error);
        }
    }

    async function bind(stage, api) {
        ui = api;
        raiz = stage;
        const nuevo = raiz.querySelector("[data-nuevo]");
        nuevo.hidden = !ui.puede("roles", "crear");
        nuevo.addEventListener("click", () => abrirFormulario(null));
        raiz.querySelector("[data-lista]").addEventListener("click", event => {
            const boton = event.target.closest("[data-action]");
            if (!boton) return;
            const rol = roles.find(r => String(r[config.id]) === boton.closest("[data-row]")?.dataset.row);
            if (!rol) return;
            if (boton.dataset.action === "editar") abrirFormulario(rol);
            if (boton.dataset.action === "eliminar") eliminar(rol);
        });
        await cargar();
    }

    const skeleton = () => window.LuminisSk.lista({ filtros: 0, columnas: 3 });
    return { render, bind, skeleton, reset: () => null };
}
