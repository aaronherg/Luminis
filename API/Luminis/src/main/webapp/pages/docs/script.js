(() => {
    const ruta = location.pathname;
    const indice = ruta.indexOf("/pages/");
    const contexto = indice > 0 ? ruta.slice(0, indice) : "";
    const seguro = location.protocol === "https:";
    const host = location.host || "localhost:8081";
    const ws = `${seguro ? "wss" : "ws"}://${host}${contexto}/socket`;
    const http = `${location.protocol.startsWith("http") ? location.protocol : "http:"}//${host}${contexto}/api/luminis`;

    document.querySelectorAll("pre code").forEach(codigo => {
        const recorrer = document.createTreeWalker(codigo, NodeFilter.SHOW_TEXT);
        while (recorrer.nextNode()) {
            const nodo = recorrer.currentNode;
            if (nodo.nodeValue.includes("{{")) nodo.nodeValue = nodo.nodeValue.replaceAll("{{WS}}", ws).replaceAll("{{HTTP}}", http);
        }
    });
    document.querySelectorAll("[data-ws-base]").forEach(elemento => (elemento.textContent = ws));
    document.querySelectorAll("[data-http-base]").forEach(elemento => (elemento.textContent = http));

    async function copiar(texto, boton) {
        const original = boton.textContent;
        try {
            await navigator.clipboard.writeText(texto);
            boton.textContent = "Copiado";
            boton.classList.add("is-copied");
        } catch (_) {
            boton.textContent = "No disponible";
        }
        window.setTimeout(() => {
            boton.textContent = original;
            boton.classList.remove("is-copied");
        }, 1400);
    }

    document.querySelectorAll("[data-copy-code]").forEach(boton => boton.addEventListener("click", () => {
        const bloque = boton.closest("[data-code], [data-tabs]");
        const visible = bloque?.querySelector("pre:not([hidden]) code");
        copiar(visible?.innerText || "", boton);
    }));
    document.querySelectorAll("[data-copy-text]").forEach(boton => boton.addEventListener("click", () => {
        copiar(boton.parentElement.querySelector("code")?.textContent || "", boton);
    }));

    document.querySelectorAll("[data-tabs]").forEach(grupo => {
        grupo.querySelectorAll("[data-tab]").forEach(boton => boton.addEventListener("click", () => {
            grupo.querySelectorAll("[data-tab]").forEach(otro => otro.classList.toggle("is-active", otro === boton));
            grupo.querySelectorAll("[data-panel]").forEach(panel => (panel.hidden = panel.dataset.panel !== boton.dataset.tab));
        }));
    });

    const menu = document.querySelector("[data-menu]");
    const nav = document.querySelector("[data-nav]");
    menu?.addEventListener("click", () => {
        const abierto = nav.classList.toggle("is-open");
        menu.setAttribute("aria-expanded", String(abierto));
    });

    const enlaces = [...document.querySelectorAll("[data-toc] a")];
    const secciones = enlaces.map(enlace => document.querySelector(enlace.getAttribute("href"))).filter(Boolean);
    if ("IntersectionObserver" in window && secciones.length) {
        const observador = new IntersectionObserver(entradas => {
            entradas.forEach(entrada => {
                if (!entrada.isIntersecting) return;
                const id = `#${entrada.target.id}`;
                enlaces.forEach(enlace => enlace.classList.toggle("is-active", enlace.getAttribute("href") === id));
            });
        }, { rootMargin: "-20% 0px -70% 0px" });
        secciones.forEach(seccion => observador.observe(seccion));
    }
})();
