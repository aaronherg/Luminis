window.RolClienteService = {
    listar: (clienteId = null) => LuminisApi.post("/api/clientes/roles/getAll", { clienteId }),
    obtener: rolClienteId => LuminisApi.post("/api/clientes/roles/getById", { rolClienteId }),
    agregar: rol => LuminisApi.post("/api/clientes/roles/add", rol),
    actualizar: rol => LuminisApi.put("/api/clientes/roles/update", rol),
    eliminar: rolClienteId => LuminisApi.delete("/api/clientes/roles/delete", { rolClienteId })
};
