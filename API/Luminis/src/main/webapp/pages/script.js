(() => {
    const movimientoReducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const formato = new Intl.NumberFormat("es-DO");

    const red = { conexiones: 0, eventosHoy: 0, porSegundo: 0, tasa: 100, proyectos: 0, anterior: null };

    function escapar(valor) {
        return String(valor ?? "").replace(/[&<>"']/g, caracter => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[caracter]);
    }

    function iniciarCabecera() {
        const cabecera = document.querySelector("[data-header]");
        const menu = document.querySelector("[data-menu]");
        const nav = document.querySelector("[data-nav]");
        const alDesplazar = () => cabecera?.classList.toggle("is-scrolled", window.scrollY > 24);
        window.addEventListener("scroll", alDesplazar, { passive: true });
        alDesplazar();
        menu?.addEventListener("click", () => {
            const abierto = nav.classList.toggle("is-open");
            menu.setAttribute("aria-expanded", String(abierto));
        });
        nav?.querySelectorAll("a").forEach(enlace => enlace.addEventListener("click", () => {
            nav.classList.remove("is-open");
            menu?.setAttribute("aria-expanded", "false");
        }));
    }

    function iniciarRevelado() {
        const elementos = document.querySelectorAll("[data-reveal]");
        if (movimientoReducido || !("IntersectionObserver" in window)) {
            elementos.forEach(elemento => elemento.classList.add("is-visible"));
            return;
        }
        const observador = new IntersectionObserver(entradas => entradas.forEach(entrada => {
            if (!entrada.isIntersecting) return;
            entrada.target.classList.add("is-visible");
            observador.unobserve(entrada.target);
        }), { threshold: 0.12 });
        elementos.forEach(elemento => observador.observe(elemento));
    }

    function iniciarBrillos() {
        document.querySelectorAll("[data-glow]").forEach(tarjeta => tarjeta.addEventListener("pointermove", evento => {
            const caja = tarjeta.getBoundingClientRect();
            tarjeta.style.setProperty("--x", `${evento.clientX - caja.left}px`);
            tarjeta.style.setProperty("--y", `${evento.clientY - caja.top}px`);
        }));
    }

    async function copiar(texto, boton, original) {
        try {
            await navigator.clipboard.writeText(texto);
            if (original) boton.textContent = "Copiado";
            boton.classList.add("is-copied");
        } catch {
            if (original) boton.textContent = "No disponible";
        }
        window.setTimeout(() => {
            if (original) boton.textContent = original;
            boton.classList.remove("is-copied");
        }, 1400);
    }

    function iniciarCodigo() {
        document.querySelectorAll("[data-code-tab]").forEach(boton => boton.addEventListener("click", () => {
            const nombre = boton.dataset.codeTab;
            document.querySelectorAll("[data-code-tab]").forEach(tab => tab.classList.toggle("is-active", tab === boton));
            document.querySelectorAll("[data-code-panel]").forEach(panel => panel.hidden = panel.dataset.codePanel !== nombre);
        }));
        const botonCodigo = document.querySelector("[data-copy-code]");
        botonCodigo?.addEventListener("click", () => {
            const panel = document.querySelector("[data-code-panel]:not([hidden]) code");
            copiar(panel?.innerText || "", botonCodigo, "Copiar");
        });
        const botonEndpoint = document.querySelector("[data-copy-endpoint]");
        botonEndpoint?.addEventListener("click", () => {
            const protocolo = location.protocol === "https:" ? "wss://" : "ws://";
            copiar(`${protocolo}${location.host}/socket?apikey=lm_xxx&usuario=sensor-01`, botonEndpoint);
        });
        document.querySelector("[data-year]")?.replaceChildren(String(new Date().getFullYear()));
    }

    function iniciarEstrellas() {
        const lienzo = document.querySelector("[data-stars]");
        const contexto = lienzo?.getContext("2d");
        if (!contexto) return;
        const dibujar = () => {
            const escala = Math.min(window.devicePixelRatio || 1, 2);
            lienzo.width = window.innerWidth * escala;
            lienzo.height = window.innerHeight * escala;
            contexto.setTransform(escala, 0, 0, escala, 0, 0);
            contexto.clearRect(0, 0, window.innerWidth, window.innerHeight);
            const total = Math.round(window.innerWidth * window.innerHeight / 5200);
            for (let i = 0; i < total; i++) {
                const brillo = Math.random();
                contexto.fillStyle = brillo > 0.94 ? "rgba(255,214,120,.9)" : `rgba(190,210,255,${0.15 + brillo * 0.55})`;
                const tamano = brillo > 0.97 ? 1.6 : brillo > 0.8 ? 1.1 : 0.7;
                contexto.beginPath();
                contexto.arc(Math.random() * window.innerWidth, Math.random() * window.innerHeight, tamano, 0, Math.PI * 2);
                contexto.fill();
            }
        };
        let espera;
        window.addEventListener("resize", () => {
            window.clearTimeout(espera);
            espera = window.setTimeout(dibujar, 200);
        }, { passive: true });
        dibujar();
    }

    function animarNumero(elemento, destino, sufijo = "") {
        const inicio = Number(elemento.dataset.valor || 0);
        elemento.dataset.valor = String(destino);
        if (movimientoReducido || inicio === destino) {
            elemento.textContent = formato.format(destino) + sufijo;
            return;
        }
        const comienzo = performance.now();
        const paso = ahora => {
            const avance = Math.min(1, (ahora - comienzo) / 900);
            const suave = 1 - Math.pow(1 - avance, 3);
            const valor = inicio + (destino - inicio) * suave;
            elemento.textContent = formato.format(Number.isInteger(destino) ? Math.round(valor) : +valor.toFixed(1)) + sufijo;
            if (avance < 1) requestAnimationFrame(paso);
        };
        requestAnimationFrame(paso);
    }

    function pintarRed() {
        document.querySelectorAll("[data-net]").forEach(elemento => {
            const clave = elemento.dataset.net;
            animarNumero(elemento, red[clave] ?? 0, clave === "tasa" ? "%" : "");
        });
    }

    async function consultarRed() {
        if (!window.ResumenService?.red) return;
        try {
            const respuesta = await ResumenService.red();
            const datos = respuesta?.data ?? respuesta;
            if (!datos || typeof datos !== "object") return;
            red.conexiones = Number(datos.conexiones || 0);
            red.eventosHoy = Number(datos.eventosHoy || 0);
            red.proyectos = Number(datos.proyectos || 0);
            const eventos30 = Number(datos.eventos30 || 0);
            red.tasa = eventos30 > 0 ? Math.min(100, +(Number(datos.entregados30 || 0) / eventos30 * 100).toFixed(1)) : 100;
            const actual = { total: Number(datos.eventosProcesados || 0), momento: Number(datos.momento || Date.now()) };
            if (red.anterior && actual.momento > red.anterior.momento && actual.total >= red.anterior.total) {
                red.porSegundo = +((actual.total - red.anterior.total) / ((actual.momento - red.anterior.momento) / 1000)).toFixed(1);
            }
            red.anterior = actual;
            pintarRed();
        } catch {
            return;
        }
    }

    function textoLimite(valor, unidad = "") {
        return valor == null ? "∞" : `${formato.format(Number(valor))}${unidad}`;
    }

    function textoRetencion(segundos) {
        const valor = Number(segundos || 0);
        if (valor >= 86400) return `${Math.round(valor / 86400)} d`;
        if (valor >= 3600) return `${Math.round(valor / 3600)} h`;
        if (valor >= 60) return `${Math.round(valor / 60)} min`;
        return `${valor} s`;
    }

    async function cargarPlanes() {
        const contenedor = document.querySelector("[data-plans]");
        const vacio = document.querySelector("[data-plans-empty]");
        if (!contenedor) return;
        try {
            if (!window.PlanService) throw new Error();
            const respuesta = await PlanService.publicos();
            const planes = respuesta?.data ?? respuesta ?? [];
            contenedor.replaceChildren();
            if (!Array.isArray(planes) || planes.length === 0) {
                vacio?.removeAttribute("hidden");
                return;
            }
            const check = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="m5 12 5 5L20 7"/></svg>';
            planes.forEach(plan => {
                const tarjeta = document.createElement("article");
                tarjeta.className = `lm-plan${plan.destacado ? " is-featured" : ""}`;
                tarjeta.dataset.reveal = "";
                const simbolo = plan.monedaSimbolo || "US$";
                const precio = Number(plan.precio || 0);
                const caracteristicas = String(plan.caracteristicas || "").split(/[|\n;]/).map(item => item.trim()).filter(Boolean);
                const especificacion = (valor, etiqueta, unidad) =>
                    `<div><strong class="${valor == null ? "is-infinite" : ""}">${textoLimite(valor, unidad)}</strong><span>${etiqueta}</span></div>`;
                const extra = Number(plan.precioMilAdicional) > 0 && plan.peticionesMes != null
                    ? `<li>${check}<span>${escapar(simbolo)} ${Number(plan.precioMilAdicional).toFixed(2)} por cada 1,000 peticiones adicionales</span></li>`
                    : "";
                tarjeta.innerHTML = `
                    ${plan.destacado ? '<span class="lm-plan-flag">El más elegido</span>' : ""}
                    <h3>${escapar(plan.nombre)}</h3>
                    <p class="lm-plan-desc">${escapar(plan.descripcion || "")}</p>
                    <p class="lm-plan-price"><strong>${precio === 0 ? "Gratis" : `${escapar(simbolo)} ${precio.toFixed(precio % 1 ? 2 : 0)}`}</strong>${precio === 0 ? "" : "<span>/ mes</span>"}</p>
                    <div class="lm-plan-specs">
                        ${especificacion(plan.peticionesMes, "peticiones al mes", "")}
                        ${especificacion(plan.conexionesSimultaneas, "conexiones simultáneas", "")}
                        ${especificacion(plan.tamanoMaximoMensajeKb, "tamaño por evento", " KB")}
                        <div><strong>${textoRetencion(plan.retencionSegundos)}</strong><span>cola offline</span></div>
                    </div>
                    <ul>${caracteristicas.map(item => `<li>${check}<span>${escapar(item)}</span></li>`).join("")}${extra}</ul>
                    <a class="fr-btn ${plan.destacado ? "fr-btn-gold" : "fr-btn-primary"}" href="registro/?plan=${encodeURIComponent(plan.planId)}">Comenzar con ${escapar(plan.nombre)}</a>`;
                contenedor.append(tarjeta);
                requestAnimationFrame(() => tarjeta.classList.add("is-visible"));
            });
        } catch {
            contenedor.replaceChildren();
            vacio?.removeAttribute("hidden");
        }
    }

    iniciarCabecera();
    iniciarRevelado();
    iniciarBrillos();
    iniciarCodigo();
    iniciarEstrellas();
    const globo = window.LuminisGlobo?.crear({
        contenedor: document.querySelector("[data-globe-wrap]"),
        lienzo: document.querySelector("[data-globe]"),
        tarjetas: [...document.querySelectorAll("[data-event-card]")],
        terminal: document.querySelector("[data-terminal]"),
        ritmo: () => Math.min(7, 1.1 + Math.log10(1 + red.porSegundo) * 2.2 + Math.log10(1 + red.conexiones) * 0.6)
    });
    if (globo) for (let i = 0; i < 4; i++) window.setTimeout(globo.lanzar, i * 250);
    consultarRed();
    window.setInterval(() => {
        if (!document.hidden) consultarRed();
    }, 8000);
    cargarPlanes();
})();
