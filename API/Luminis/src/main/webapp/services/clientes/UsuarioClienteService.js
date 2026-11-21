window.UsuarioClienteService = {
    listar: (clienteId = null) => LuminisApi.post("/api/clientes/usuarios/getAll", { clienteId }),
    obtener: usuarioClienteId => LuminisApi.post("/api/clientes/usuarios/getById", { usuarioClienteId }),
    agregar: usuario => LuminisApi.post("/api/clientes/usuarios/add", usuario),
    actualizar: usuario => LuminisApi.put("/api/clientes/usuarios/update", usuario),
    restablecerContrasena: (usuarioClienteId, contrasena) =>
        LuminisApi.put("/api/clientes/usuarios/restablecerContrasena", { usuarioClienteId, contrasena }),
    eliminar: usuarioClienteId => LuminisApi.delete("/api/clientes/usuarios/delete", { usuarioClienteId })
};
