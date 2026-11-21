window.PagoSuscripcionService = {
    paginar: (filtro = {}) => LuminisApi.post("/api/suscripciones/pagos/getAllPages", filtro),
    obtener: pagoSuscripcionId => LuminisApi.post("/api/suscripciones/pagos/getById", { pagoSuscripcionId }),
    registrar: pago => LuminisApi.post("/api/suscripciones/pagos/add", pago)
};
