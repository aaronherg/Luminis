import { crearPantallaUsuarios } from "../../../comunes/usuarios.js?v=20261122-5";

const pantalla = crearPantallaUsuarios({
    servicio: UsuarioAdministracionService,
    rolesServicio: () => RolAdministracionService.listar(),
    id: "usuarioAdministracionId",
    uuid: "usuarioAdministracionUuid",
    rolId: "rolAdministracionId",
    accionContrasena: "restablecer_contrasena"
});

export const render = pantalla.render;
export const bind = pantalla.bind;
export const reset = pantalla.reset;
