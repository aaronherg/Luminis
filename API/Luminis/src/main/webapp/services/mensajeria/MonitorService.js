window.MonitorService = {
    listar: (clienteId = null) => LuminisApi.post("/api/mensajeria/monitor/getAll", { clienteId }, { silent: true }),
    desconectar: datos => LuminisApi.delete("/api/mensajeria/monitor/delete", datos)
};