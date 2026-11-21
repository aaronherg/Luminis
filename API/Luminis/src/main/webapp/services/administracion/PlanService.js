window.PlanService = {
    listar: () => LuminisApi.get("/api/administracion/planes/getAll"),
    publicos: () => LuminisApi.get("/api/administracion/planes/getAllPublicos", { silent: true }),
    obtener: planId => LuminisApi.post("/api/administracion/planes/getById", { planId }),
    agregar: plan => LuminisApi.post("/api/administracion/planes/add", plan),
    actualizar: plan => LuminisApi.put("/api/administracion/planes/update", plan),
    eliminar: planId => LuminisApi.delete("/api/administracion/planes/delete", { planId })
};
