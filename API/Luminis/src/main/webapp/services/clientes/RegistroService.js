window.RegistroService = {
    registrar: datos => LuminisApi.post("/api/clientes/registro/add", datos)
};
