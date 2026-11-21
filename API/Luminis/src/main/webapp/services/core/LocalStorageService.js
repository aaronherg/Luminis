window.LuminisStorage = {
    sessionKey: "luminis.auth.session",

    guardarSesion(session) {
        if (!session || !session.token) throw new Error("La respuesta no contiene una sesión válida.");
        localStorage.setItem(this.sessionKey, JSON.stringify(session));
    },

    actualizarSesion(cambios) {
        const actual = this.sesion();
        if (actual) localStorage.setItem(this.sessionKey, JSON.stringify({ ...actual, ...cambios }));
    },

    sesion() {
        try {
            return JSON.parse(localStorage.getItem(this.sessionKey) || "null");
        } catch (_) {
            return null;
        }
    },

    token() {
        const token = this.sesion()?.token;
        return typeof token === "string" ? token : null;
    },

    limpiar() {
        localStorage.removeItem(this.sessionKey);
    }
};
