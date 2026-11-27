import { crearPantallaRoles } from "../../../comunes/roles.js?v=20261127-12";

const pantalla = crearPantallaRoles({
    servicio: RolClienteService,
    modulos: () => ModuloClienteService.listar(),
    id: "rolClienteId",
    uuid: "rolClienteUuid",
});

export const render = pantalla.render;
export const skeleton = pantalla.skeleton;
export const bind = pantalla.bind;
export const reset = pantalla.reset;
