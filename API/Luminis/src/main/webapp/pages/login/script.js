(() => {
    const form = document.querySelector("[data-login-form]");
    const errorBox = document.querySelector("[data-login-error]");
    const submit = form?.querySelector('button[type="submit"]');

    const sesion = LuminisStorage.sesion();
    if (sesion?.token) {
        AutenticacionService.validarSesion()
            .then(actual => {
                if (actual) window.location.replace(AutenticacionService.destino(actual));
            })
            .catch(() => LuminisStorage.limpiar());
    }

    document.querySelector("[data-password-toggle]")?.addEventListener("click", event => {
        const input = form.elements.contrasena;
        const visible = input.type === "text";
        input.type = visible ? "password" : "text";
        event.currentTarget.setAttribute("aria-label", visible ? "Mostrar contraseña" : "Ocultar contraseña");
        event.currentTarget.title = visible ? "Mostrar contraseña" : "Ocultar contraseña";
    });

    function mostrarError(mensaje) {
        errorBox.textContent = mensaje;
        errorBox.hidden = !mensaje;
    }

    form?.addEventListener("submit", async event => {
        event.preventDefault();
        const usuario = form.elements.usuario.value.trim();
        const contrasena = form.elements.contrasena.value;
        form.elements.usuario.classList.toggle("is-invalid", !usuario);
        form.elements.contrasena.classList.toggle("is-invalid", !contrasena);
        if (!usuario || !contrasena) {
            mostrarError("Ingresa tu usuario y contraseña.");
            return;
        }
        mostrarError("");
        submit.disabled = true;
        submit.classList.add("is-busy");
        try {
            const session = await AutenticacionService.login(usuario, contrasena);
            window.location.replace(AutenticacionService.destino(session));
        } catch (error) {
            mostrarError(error.message);
            AppNotifications.error(error.message);
        } finally {
            submit.disabled = false;
            submit.classList.remove("is-busy");
        }
    });
})();
