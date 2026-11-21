window.ResumenService = {
    administracion: () => LuminisApi.get("/api/general/resumen/getAdministracion"),
    cliente: () => LuminisApi.get("/api/general/resumen/getCliente"),
    red: () => LuminisApi.get("/api/general/resumen/getRed", { silent: true })
};
