window.SuscripcionSolicitudService = {
    paginar: (filtro = {}) => LuminisApi.post("/api/suscripciones/solicitudes/getAllPages", filtro),
    obtener: suscripcionSolicitudId => LuminisApi.post("/api/suscripciones/solicitudes/getById", { suscripcionSolicitudId }),
    solicitar: datos => LuminisApi.post("/api/suscripciones/solicitudes/add", datos),
    responder: datos => LuminisApi.put("/api/suscripciones/solicitudes/update", datos),
    cancelar: suscripcionSolicitudId => LuminisApi.delete("/api/suscripciones/solicitudes/delete", { suscripcionSolicitudId })
};
