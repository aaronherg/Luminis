(() => {
    const version = "20261122-6";
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
            perfil: "./pantallas/perfil/script.js",
            __perfil: "./pantallas/perfilusuario/script.js"
        }
    });
})();