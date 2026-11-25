window.AutenticacionService = {
    async login(usuario, contrasena) {
        const resultado = await LuminisApi.post("/api/seguridad/autenticacion/login", {
            usuario,
            contrasena,
            agente: navigator.userAgent
        });
        if (resultado?.requiereDosFactores) return resultado;
        if (!resultado?.token || !resultado?.tipo) throw new Error("El servidor respondió sin los datos completos de la sesión.");
        LuminisStorage.guardarSesion(resultado);
        return resultado;
    },

    async ingresoDosFactores(usuario, contrasena, codigo) {
        const session = await LuminisApi.post("/api/seguridad/autenticacion/dosfactores/ingreso", {
            usuario,
            contrasena,
            codigo,
            agente: navigator.userAgent
        });
        if (!session?.token || !session?.tipo) throw new Error("El servidor respondió sin los datos completos de la sesión.");
        LuminisStorage.guardarSesion(session);
        return session;
    },

    sesionActual() {
        return LuminisStorage.sesion();
    },

    destino(session = LuminisStorage.sesion()) {
        const contexto = window.LuminisConfig?.contextPath || "";
        return `${contexto}${session?.tipo === "Administracion" ? "/pages/center/" : "/pages/customer/"}`;
    },

    async validarSesion(tipoEsperado) {
        const session = LuminisStorage.sesion();
        if (!session?.token || (tipoEsperado && session.tipo !== tipoEsperado)) return null;
        const actual = await LuminisApi.post("/api/seguridad/autenticacion/getSesion", {}, { silent: true });
        LuminisStorage.actualizarSesion(actual || {});
        return LuminisStorage.sesion();
    },

    cambiarContrasena: (contrasenaActual, contrasenaNueva) =>
        LuminisApi.put("/api/seguridad/autenticacion/cambiarContrasena", { contrasenaActual, contrasenaNueva }),

    async logout() {
        try {
            if (LuminisStorage.token()) await LuminisApi.put("/api/seguridad/autenticacion/logout", {}, { silent: true });
        } catch (_) {
            LuminisStorage.limpiar();
        }
        LuminisStorage.limpiar();
    },

    puede(accion) {
        return (LuminisStorage.sesion()?.acciones || []).includes(accion);
    }
};
