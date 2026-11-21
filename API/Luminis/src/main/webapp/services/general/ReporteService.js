window.ReporteService = {
    ingresos: filtro => LuminisApi.post("/api/general/reportes/getIngresos", filtro || {}),
    cuentasPorCobrar: () => LuminisApi.post("/api/general/reportes/getCuentasCobrar", {}),
    consumoClientes: filtro => LuminisApi.post("/api/general/reportes/getConsumoClientes", filtro || {}),
    trafico: filtro => LuminisApi.post("/api/general/reportes/getTrafico", filtro || {}),
    consumo: filtro => LuminisApi.post("/api/general/reportes/getConsumo", filtro || {}),
    entregas: filtro => LuminisApi.post("/api/general/reportes/getEntregas", filtro || {})
};