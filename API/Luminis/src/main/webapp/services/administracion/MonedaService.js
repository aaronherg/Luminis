window.MonedaService = {
    listar: () => LuminisApi.get("/api/administracion/monedas/getAll"),
    obtener: monedaId => LuminisApi.post("/api/administracion/monedas/getById", { monedaId }),
    agregar: moneda => LuminisApi.post("/api/administracion/monedas/add", moneda),
    actualizar: moneda => LuminisApi.put("/api/administracion/monedas/update", moneda),
    eliminar: monedaId => LuminisApi.delete("/api/administracion/monedas/delete", { monedaId })
};
