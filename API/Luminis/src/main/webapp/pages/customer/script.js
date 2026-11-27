(() => {
    const version = "20261127-12";
    LuminisPanel.iniciar({
        tipo: "Cliente",
        version,
        pantallas: {
            inicio: "./pantallas/inicio/script.js",
            monitor: "./pantallas/monitor/script.js",
            consola: "./pantallas/consola/script.js",
            suscripcion: "./pantallas/suscripcion/script.js",
            pagos: "./pantallas/pagos/script.js",
            reporte_consumo: "./pantallas/reporte-consumo/script.js",
            reporte_entregas: "./pantallas/reporte-entregas/script.js",
            apikeys: "./pantallas/apikeys/script.js",
            usuarios: "./pantallas/usuarios/script.js",
            roles: "./pantallas/roles/script.js",
            __perfil: "./pantallas/perfilusuario/script.js",
            __seguridad: "./pantallas/seguridad/script.js"
        }
    });
})();