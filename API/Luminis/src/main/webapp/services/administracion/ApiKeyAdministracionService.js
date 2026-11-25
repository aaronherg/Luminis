window.ApiKeyAdministracionService = {
    listar: () => LuminisApi.post("/api/administracion/apikeys/getAll", {}),
    crear: nombre => LuminisApi.post("/api/administracion/apikeys/add", { nombre }),
    renombrar: (uuid, nombre) => LuminisApi.put("/api/administracion/apikeys/rename", { uuid, nombre }),
    revocar: uuid => LuminisApi.put("/api/administracion/apikeys/revoke", { uuid })
};
