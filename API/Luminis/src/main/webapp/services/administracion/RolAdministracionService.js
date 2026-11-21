window.RolAdministracionService = {
    listar: () => LuminisApi.get("/api/administracion/roles/getAll"),
    obtener: rolAdministracionId => LuminisApi.post("/api/administracion/roles/getById", { rolAdministracionId }),
    agregar: rol => LuminisApi.post("/api/administracion/roles/add", rol),
    actualizar: rol => LuminisApi.put("/api/administracion/roles/update", rol),
    eliminar: rolAdministracionId => LuminisApi.delete("/api/administracion/roles/delete", { rolAdministracionId })
};
