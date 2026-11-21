window.ApiKeyService = {
    listar: (clienteId = null) => LuminisApi.post("/api/mensajeria/apikeys/getAll", { clienteId }),
    permisos: () => LuminisApi.get("/api/mensajeria/apikeys/getPermisos", { silent: true }),
    agregar: datos => LuminisApi.post("/api/mensajeria/apikeys/add", datos),
    actualizar: datos => LuminisApi.put("/api/mensajeria/apikeys/update", datos),
    revocar: apiKeyId => LuminisApi.delete("/api/mensajeria/apikeys/delete", { apiKeyId })
};
