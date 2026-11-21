window.UsuarioAdministracionService = {
    listar: () => LuminisApi.get("/api/administracion/usuarios/getAll"),
    obtener: usuarioAdministracionId => LuminisApi.post("/api/administracion/usuarios/getById", { usuarioAdministracionId }),
    agregar: usuario => LuminisApi.post("/api/administracion/usuarios/add", usuario),
    actualizar: usuario => LuminisApi.put("/api/administracion/usuarios/update", usuario),
    restablecerContrasena: (usuarioAdministracionId, contrasena) =>
        LuminisApi.put("/api/administracion/usuarios/restablecerContrasena", { usuarioAdministracionId, contrasena }),
    eliminar: usuarioAdministracionId => LuminisApi.delete("/api/administracion/usuarios/delete", { usuarioAdministracionId })
};
