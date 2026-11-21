(() => {
    "use strict";

    const types = {
        exito: { method: "success", title: "Listo" },
        info: { method: "info", title: "Aviso" },
        advertencia: { method: "warning", title: "Revisa esto" },
        error: { method: "error", title: "No se pudo completar" }
    };

    if (window.toastr) {
        window.toastr.options = {
            closeButton: true,
            closeOnHover: true,
            newestOnTop: true,
            preventDuplicates: false,
            progressBar: true,
            positionClass: "toast-bottom-right",
            timeOut: 4800,
            extendedTimeOut: 1800,
            showDuration: 220,
            hideDuration: 240,
            escapeHtml: true,
            target: "body"
        };
    }

    function show(message, options = {}) {
        const text = String(message ?? "").trim();
        if (!text) return null;
        const key = String(options.type || "info").toLocaleLowerCase("es");
        const config = types[key] || types.info;
        const title = options.title || config.title;
        if (window.toastr && typeof window.toastr[config.method] === "function") {
            return window.toastr[config.method](text, title, {
                timeOut: Number.isFinite(options.duration) ? options.duration : window.toastr.options.timeOut,
                progressBar: true,
                escapeHtml: true
            });
        }
        window.dispatchEvent(new CustomEvent("luminis:notification", { detail: { message: text, title, type: key } }));
        return null;
    }

    window.AppNotifications = Object.freeze({
        show,
        success: (message, title) => show(message, { type: "exito", title }),
        info: (message, title) => show(message, { type: "info", title }),
        warning: (message, title) => show(message, { type: "advertencia", title }),
        error: (message, title) => show(message, { type: "error", title })
    });
    window.notificacion = (asunto, mensaje, _audio, tipo = "Info") => {
        const key = String(tipo).toLocaleLowerCase("es");
        const type = key === "exito" ? "exito" : key === "advertencia" ? "advertencia" : key === "error" ? "error" : "info";
        return show(mensaje, { type, title: asunto || undefined });
    };
})();
