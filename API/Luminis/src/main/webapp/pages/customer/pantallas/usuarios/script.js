import { crearPantallaUsuarios } from "../../../comunes/usuarios.js?v=20261122-2";

const pantalla = crearPantallaUsuarios({
    servicio: UsuarioClienteService,
    rolesServicio: () => RolClienteService.listar(),
    id: "usuarioClienteId",
    uuid: "usuarioClienteUuid",
    rolId: "rolClienteId",
    accionContrasena: "modificar"
});

export const render = pantalla.render;
export const bind = pantalla.bind;
export const reset = pantalla.reset;
