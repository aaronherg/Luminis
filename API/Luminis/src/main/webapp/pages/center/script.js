(() => {
    const version = "20261127-14";
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
            apikeysadmin: "./pantallas/apikeysadmin/script.js",
            smtp: "./pantallas/smtp/script.js",
            whatsapp: "./pantallas/whatsapp/script.js",
            __seguridad: "./pantallas/seguridad/script.js",
            __perfil: "./pantallas/perfilusuario/script.js",
            reporte_ingresos: "./pantallas/reporte-ingresos/script.js",
            reporte_cobros: "./pantallas/reporte-cobros/script.js",
            reporte_consumo: "./pantallas/reporte-consumo/script.js",
            reporte_trafico: "./pantallas/reporte-trafico/script.js"
        }
    });
})();