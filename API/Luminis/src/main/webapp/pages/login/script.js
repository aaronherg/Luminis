(() => {
    const form = document.querySelector("[data-login-form]");
    const errorBox = document.querySelector("[data-login-error]");
    const submit = form?.querySelector('button[type="submit"]');
    const card = form?.closest(".auth-card") || document.querySelector(".auth-card");

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

    function mostrarDosFactores(usuario, contrasena, correo) {
        const otros = [...card.children].filter(e => !e.hasAttribute("data-2fa"));
        otros.forEach(e => (e.hidden = true));
        let panel = card.querySelector("[data-2fa]");
        if (!panel) {
            panel = document.createElement("div");
            panel.className = "auth-form";
            panel.setAttribute("data-2fa", "");
            card.appendChild(panel);
        }
        panel.hidden = false;
        panel.innerHTML = `
            <h1 style="margin:0 0 6px">Verifica tu identidad</h1>
            <p class="auth-subtitle">Enviamos un código de 6 caracteres a <strong>${correo || "tu correo"}</strong>. Escríbelo para continuar.</p>
            <div data-cajas style="display:flex;gap:8px;justify-content:center;margin:18px 0">
                ${'<input inputmode="text" maxlength="1" autocomplete="one-time-code" class="fr-input" style="width:46px;height:56px;text-align:center;font-size:1.4rem;font-weight:800;text-transform:uppercase">'.repeat(6)}
            </div>
            <p class="auth-error" data-2fa-error role="alert" hidden></p>
            <button class="fr-btn fr-btn-primary" type="button" data-2fa-ok style="width:100%">Verificar e ingresar</button>
            <p class="auth-switch" style="margin-top:14px">¿No te llegó? <button type="button" data-2fa-reenviar style="background:none;border:0;color:var(--accent);text-decoration:underline;cursor:pointer;font:inherit">Reenviar código</button> · <button type="button" data-2fa-volver style="background:none;border:0;color:var(--text-secondary);text-decoration:underline;cursor:pointer;font:inherit">Volver</button></p>`;

        const cajas = [...panel.querySelectorAll("[data-cajas] input")];
        const okBtn = panel.querySelector("[data-2fa-ok]");
        const err2 = panel.querySelector("[data-2fa-error]");
        const valor = () => cajas.map(c => c.value).join("");
        const err = m => { err2.textContent = m; err2.hidden = !m; };
        cajas.forEach((c, i) => {
            c.addEventListener("input", () => { c.value = c.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(-1); if (c.value && cajas[i + 1]) cajas[i + 1].focus(); if (cajas.every(x => x.value)) okBtn.click(); });
            c.addEventListener("keydown", e => { if (e.key === "Backspace" && !c.value && cajas[i - 1]) cajas[i - 1].focus(); else if (e.key === "Enter") okBtn.click(); });
            c.addEventListener("paste", e => { const t = (e.clipboardData?.getData("text") || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6); if (!t) return; e.preventDefault(); t.split("").forEach((d, j) => { if (cajas[j]) cajas[j].value = d; }); (cajas[t.length] || cajas[5]).focus(); if (t.length === 6) okBtn.click(); });
        });
        setTimeout(() => cajas[0]?.focus(), 60);

        let ocupado = false;
        okBtn.addEventListener("click", async () => {
            if (ocupado) return;
            if (valor().length !== 6) { err("Escribe el código completo de 6 caracteres."); return; }
            err("");
            ocupado = true; okBtn.disabled = true; okBtn.classList.add("is-busy");
            try {
                const session = await AutenticacionService.ingresoDosFactores(usuario, contrasena, valor());
                window.location.replace(AutenticacionService.destino(session));
            } catch (error) {
                err(error.message);
                cajas.forEach(c => (c.value = ""));
                cajas[0]?.focus();
            } finally {
                ocupado = false; okBtn.disabled = false; okBtn.classList.remove("is-busy");
            }
        });
        panel.querySelector("[data-2fa-reenviar]").addEventListener("click", async e => {
            const boton = e.currentTarget;
            boton.disabled = true;
            try { const r = await AutenticacionService.login(usuario, contrasena); err(""); if (r?.correo) panel.querySelector(".auth-subtitle strong").textContent = r.correo; AppNotifications.success("Código reenviado."); }
            catch (error) { err(error.message); }
            finally { setTimeout(() => (boton.disabled = false), 1500); }
        });
        panel.querySelector("[data-2fa-volver]").addEventListener("click", () => {
            panel.hidden = true;
            otros.forEach(e => (e.hidden = false));
            form.elements.contrasena.value = "";
            form.elements.usuario.focus();
        });
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
            const resultado = await AutenticacionService.login(usuario, contrasena);
            if (resultado?.requiereDosFactores) {
                mostrarDosFactores(usuario, contrasena, resultado.correo);
                return;
            }
            window.location.replace(AutenticacionService.destino(resultado));
        } catch (error) {
            mostrarError(error.message);
            AppNotifications.error(error.message);
        } finally {
            submit.disabled = false;
            submit.classList.remove("is-busy");
        }
    });
})();
