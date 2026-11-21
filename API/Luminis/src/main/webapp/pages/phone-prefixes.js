(() => {
    if (window.LuminisTelefono) return;

    const archivos = new URL("./files/", document.currentScript?.src || location.href).href;
    let catalogo = { predeterminado: "DO", preferidosPorPrefijo: {}, paises: [] };
    const cargado = fetch(`${archivos}paises/paises.json`, { cache: "force-cache" })
        .then(respuesta => (respuesta.ok ? respuesta.json() : Promise.reject(new Error("No se pudo cargar el catálogo de países."))))
        .then(datos => (catalogo = datos))
        .catch(() => catalogo);
    let abierto = null;

    const escapar = valor =>
        String(valor ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
    const normalizar = texto => String(texto || "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

    function bandera(pais) {
        return `<img class="phone-prefix-flag" src="${archivos}${pais.bandera}" alt="" loading="lazy" width="22" height="16">`;
    }

    function paisDe(prefijo, codigo) {
        const paises = catalogo.paises;
        const valor = String(prefijo || "").trim();
        if (codigo) {
            const exacto = paises.find(p => p.codigo === codigo && p.prefijo === valor);
            if (exacto) return exacto;
        }
        const preferido = catalogo.preferidosPorPrefijo?.[valor];
        return (
            (preferido && paises.find(p => p.codigo === preferido)) ||
            paises.find(p => p.prefijo === valor) ||
            paises.find(p => p.codigo === catalogo.predeterminado) ||
            paises[0]
        );
    }

    function cerrar() {
        if (!abierto) return;
        abierto.classList.remove("is-open");
        abierto.querySelector(".phone-prefix-trigger")?.setAttribute("aria-expanded", "false");
        abierto = null;
    }

    async function preparar(input) {
        if (input.dataset.phonePrefixReady) return;
        input.dataset.phonePrefixReady = "true";
        await cargado;
        if (!catalogo.paises.length) {
            input.dataset.phonePrefixReady = "";
            return;
        }
        const PAISES = catalogo.paises;
        const deshabilitado = input.disabled;
        input.type = "hidden";
        input.removeAttribute("maxlength");

        const selector = document.createElement("div");
        selector.className = "phone-prefix";
        selector.innerHTML = `
            <button class="phone-prefix-trigger" type="button" aria-haspopup="listbox" aria-expanded="false" ${deshabilitado ? "disabled" : ""}>
                <span class="phone-prefix-current" data-i18n-skip></span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
            </button>
            <div class="phone-prefix-panel" role="listbox">
                <input class="phone-prefix-search" type="search" placeholder="Buscar país o código" aria-label="Buscar país o código" autocomplete="off">
                <ul class="phone-prefix-list" data-i18n-skip>${PAISES.map(
                    p => `<li role="option" tabindex="-1" data-codigo="${p.codigo}" data-buscar="${escapar(normalizar(`${p.nombre} ${p.prefijo} ${p.codigo}`))}">${bandera(p)}<span class="phone-prefix-country">${escapar(p.nombre)}</span><span class="phone-prefix-code">${p.prefijo}</span></li>`
                ).join("")}</ul>
            </div>`;
        input.parentNode.insertBefore(selector, input);
        selector.appendChild(input);

        const actual = selector.querySelector(".phone-prefix-current");
        const buscador = selector.querySelector(".phone-prefix-search");
        const lista = selector.querySelector(".phone-prefix-list");
        let pais = paisDe(input.value, input.dataset.pais);

        function reflejar() {
            input.value = pais.prefijo;
            input.dataset.pais = pais.codigo;
            actual.innerHTML = `${bandera(pais)}<span>${pais.prefijo}</span>`;
            selector.title = `${pais.nombre} (${pais.prefijo})`;
            lista.querySelectorAll("li").forEach(li => li.classList.toggle("is-selected", li.dataset.codigo === pais.codigo));
        }

        function filtrar() {
            const texto = normalizar(buscador.value).replace(/^\+/, "+");
            lista.querySelectorAll("li").forEach(li => {
                li.hidden = Boolean(texto) && !li.dataset.buscar.includes(texto);
            });
        }

        function elegir(li) {
            pais = PAISES.find(p => p.codigo === li.dataset.codigo) || pais;
            reflejar();
            cerrar();
            input.dispatchEvent(new Event("change", { bubbles: true }));
            selector.querySelector(".phone-prefix-trigger").focus();
        }

        selector.querySelector(".phone-prefix-trigger").addEventListener("click", () => {
            if (abierto === selector) return cerrar();
            cerrar();
            abierto = selector;
            selector.classList.add("is-open");
            selector.querySelector(".phone-prefix-trigger").setAttribute("aria-expanded", "true");
            buscador.value = "";
            filtrar();
            window.setTimeout(() => {
                buscador.focus();
                lista.querySelector(".is-selected")?.scrollIntoView({ block: "center" });
            }, 20);
        });
        buscador.addEventListener("input", filtrar);
        buscador.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                const primero = [...lista.querySelectorAll("li")].find(li => !li.hidden);
                if (primero) elegir(primero);
            }
            if (event.key === "ArrowDown") {
                event.preventDefault();
                [...lista.querySelectorAll("li")].find(li => !li.hidden)?.focus();
            }
            if (event.key === "Escape") {
                event.stopPropagation();
                cerrar();
            }
        });
        lista.addEventListener("click", event => {
            const li = event.target.closest("li");
            if (li) elegir(li);
        });
        lista.addEventListener("keydown", event => {
            const li = event.target.closest("li");
            if (!li) return;
            const visibles = [...lista.querySelectorAll("li")].filter(item => !item.hidden);
            const indice = visibles.indexOf(li);
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                elegir(li);
            } else if (event.key === "ArrowDown") {
                event.preventDefault();
                visibles[indice + 1]?.focus();
            } else if (event.key === "ArrowUp") {
                event.preventDefault();
                (visibles[indice - 1] || buscador).focus();
            } else if (event.key === "Escape") {
                event.stopPropagation();
                cerrar();
            }
        });
        reflejar();
    }

    function mejorar(raiz = document) {
        if (raiz.matches?.("input[data-phone-prefix]")) preparar(raiz);
        raiz.querySelectorAll?.("input[data-phone-prefix]").forEach(preparar);
    }

    document.addEventListener("mousedown", event => {
        if (abierto && !abierto.contains(event.target)) cerrar();
    });

    new MutationObserver(registros =>
        registros.forEach(registro => registro.addedNodes.forEach(nodo => nodo.nodeType === 1 && mejorar(nodo)))
    ).observe(document.documentElement, { childList: true, subtree: true });

    mejorar();
    window.LuminisTelefono = Object.freeze({ mejorar, paises: () => catalogo.paises });
})();
