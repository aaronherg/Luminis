import { crearMonitor } from "../../../comunes/monitor.js?v=20261122-5";

const monitor = crearMonitor({ administracion: true });

export const render = monitor.render;
export const bind = monitor.bind;
export const reset = monitor.reset;

export function skeleton() {
    return window.LuminisSk.tablero({ tarjetas: 4, columnas: 5 });
}
