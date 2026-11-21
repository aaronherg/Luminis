(() => {
    const version = "20261122-2";
    LuminisPanel.iniciar({
        tipo: "Administracion",
        version,
        pantallas: {
            inicio: "./pantallas/inicio/script.js",
            clientes: "./pantallas/clientes/script.js",
            suscripciones: "./pantallas/suscripciones/script.js",
            pagos: "./pantallas/pagos/script.js",
            monitor: "./pantallas/monitor/script.js",
            planes: "./pantallas/planes/script.js",
            monedas: "./pantallas/monedas/script.js",
            usuarios: "./pantallas/usuarios/script.js",
            roles: "./pantallas/roles/script.js",
            reporte_ingresos: "./pantallas/reporte-ingresos/script.js",
            reporte_cobros: "./pantallas/reporte-cobros/script.js",
            reporte_consumo: "./pantallas/reporte-consumo/script.js",
            reporte_trafico: "./pantallas/reporte-trafico/script.js"
        }
    });
})();