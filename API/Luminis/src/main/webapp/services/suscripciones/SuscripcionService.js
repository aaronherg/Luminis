window.SuscripcionService = {
    paginar: (filtro = {}) => LuminisApi.post("/api/suscripciones/getAllPages", filtro),
    obtener: (clienteId = null) => LuminisApi.post("/api/suscripciones/getById", { clienteId }),
    asignar: datos => LuminisApi.post("/api/suscripciones/add", datos),
    cancelar: clienteId => LuminisApi.put("/api/suscripciones/cancelar", { clienteId })
};
