(() => {
    const form = document.querySelector("[data-registro-form]");
    const errorBox = document.querySelector("[data-registro-error]");
    const submit = form.querySelector('button[type="submit"]');
    const reglas = {
        largo: valor => valor.length >= 8,
        mayuscula: valor => /[A-Z]/.test(valor),
        minuscula: valor => /[a-z]/.test(valor),
        numero: valor => /\d/.test(valor),
        especial: valor => /[^A-Za-z0-9]/.test(valor)
    };
    const correoValido = valor => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);
    const usuarioValido = valor => /^[A-Za-z0-9._@-]{3,60}$/.test(valor);

    const planId = new URLSearchParams(window.location.search).get("plan");
    if (planId) {
        try {
            sessionStorage.setItem("luminis.planElegido", planId);
        } catch (_) {
            form.dataset.planVolatil = planId;
        }
        PlanService.publicos()
            .then(planes => {
                const plan = (planes || []).find(item => String(item.planId) === String(planId));
                const aviso = document.querySelector("[data-plan-elegido]");
                if (!plan || !aviso) return;
                aviso.textContent = `Plan elegido: ${plan.nombre}. Podrás solicitarlo al entrar a tu cuenta.`;
                aviso.hidden = false;
            })
            .catch(() => null);
    }

    function tipoActual() {
        return form.querySelector('input[name="tipoCliente"]:checked')?.value || "Empresa";
    }

    function aplicarTipo() {
        const empresa = tipoActual() === "Empresa";
        form.querySelectorAll("[data-solo-empresa]").forEach(campo => {
            campo.hidden = !empresa;
            if (!empresa) campo.querySelectorAll("input").forEach(entrada => (entrada.value = ""));
        });
        const etiquetar = (nombre, texto) => {
            const nodo = form.elements[nombre]?.closest(".fr-field")?.querySelector(":scope > span")?.firstChild;
            if (nodo) nodo.nodeValue = texto;
        };
        etiquetar("nombreComercial", empresa ? "Nombre comercial" : "Nombre completo");
        etiquetar("documento", empresa ? "RNC" : "Cédula");
        form.elements.nombreComercial.placeholder = empresa ? "Nombre de la empresa" : "Nombre y apellidos";
        form.elements.documento.placeholder = empresa ? "000-00000-0" : "000-0000000-0";
    }

    form.querySelectorAll('input[name="tipoCliente"]').forEach(radio => radio.addEventListener("change", aplicarTipo));
    aplicarTipo();

    form.elements.contrasena.addEventListener("input", () => {
        const valor = form.elements.contrasena.value;
        document.querySelectorAll("[data-password-rules] [data-rule]").forEach(item => {
            item.classList.toggle("is-ok", reglas[item.dataset.rule](valor));
        });
    });

    function mostrarError(mensaje, campo) {
        errorBox.textContent = mensaje;
        errorBox.hidden = !mensaje;
        if (campo) {
            campo.classList.add("is-invalid");
            campo.focus();
        }
    }

    function valor(nombre) {
        return form.elements[nombre].value.trim();
    }

    function validar() {
        form.querySelectorAll(".is-invalid").forEach(campo => campo.classList.remove("is-invalid"));
        const e = form.elements;
        if (!valor("nombreComercial")) return mostrarError("El nombre comercial es obligatorio.", e.nombreComercial), false;
        if (!correoValido(valor("correo"))) return mostrarError("Ingresa un correo válido.", e.correo), false;
        if (!valor("nombres")) return mostrarError("Los nombres del propietario son obligatorios.", e.nombres), false;
        if (!usuarioValido(valor("usuario")))
            return mostrarError("El usuario debe tener de 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @", e.usuario), false;
        const clave = e.contrasena.value;
        if (!Object.values(reglas).every(regla => regla(clave)))
            return mostrarError("La contraseña no cumple los requisitos.", e.contrasena), false;
        if (clave !== e.confirmacion.value) return mostrarError("Las contraseñas no coinciden.", e.confirmacion), false;
        return true;
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        mostrarError("");
        if (!validar()) return;
        const empresa = tipoActual() === "Empresa";
        const datos = {
            tipoCliente: tipoActual(),
            nombreComercial: valor("nombreComercial"),
            razonSocial: empresa ? valor("razonSocial") || null : null,
            documento: valor("documento") || null,
            correo: valor("correo"),
            prefijoTelefono: valor("prefijoTelefono") || null,
            telefono: valor("telefono") || null,
            direccion: valor("direccion") || null,
            nombres: valor("nombres"),
            apellidos: valor("apellidos") || null,
            usuario: valor("usuario"),
            contrasena: form.elements.contrasena.value
        };
        submit.disabled = true;
        submit.classList.add("is-busy");
        try {
            await RegistroService.registrar(datos);
            document.querySelector("[data-registro-card]").hidden = true;
            document.querySelector("[data-registro-exito]").hidden = false;
            AppNotifications.success("Cuenta creada correctamente. Ya puede iniciar sesión");
        } catch (error) {
            mostrarError(error.message);
            AppNotifications.error(error.message);
        } finally {
            submit.disabled = false;
            submit.classList.remove("is-busy");
        }
    });
})();
