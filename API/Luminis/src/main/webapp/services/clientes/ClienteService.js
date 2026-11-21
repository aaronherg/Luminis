window.ClienteService = {
    paginar: (filtro = {}) => LuminisApi.post("/api/clientes/getAllPages", filtro),
    obtener: clienteId => LuminisApi.post("/api/clientes/getById", { clienteId }),
    actual: () => LuminisApi.post("/api/clientes/getActual", {}),
    agregar: cliente => LuminisApi.post("/api/clientes/add", cliente),
    actualizar: cliente => LuminisApi.put("/api/clientes/update", cliente),
    actualizarActual: cliente => LuminisApi.put("/api/clientes/updateActual", cliente),
    eliminar: clienteId => LuminisApi.delete("/api/clientes/delete", { clienteId })
};
