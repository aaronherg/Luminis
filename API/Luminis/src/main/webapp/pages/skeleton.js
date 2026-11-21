(() => {
    if (window.LuminisSkeleton) return;

    const instances = new WeakMap();

    function skeletonRows(count, kind = "row") {
        return Array.from(
            { length: count },
            (_, index) => `<div class="fr-skeleton-${kind}" style="--row:${index}"><i></i><i></i><i></i><i></i></div>`
        ).join("");
    }

    function skeletonFields(count = 6) {
        return Array.from(
            { length: count },
            (_, index) => `<div class="fr-skeleton-field" style="--row:${index}"><i></i><b></b></div>`
        ).join("");
    }

    function viewMarkup(target) {
        const id = target?.dataset?.windowId || "";
        const classes = target?.className || "";
        if (/punto-venta/.test(id)) {
            return `<div class="fr-skeleton-pos"><section><div class="fr-skeleton-toolbar">${skeletonFields(3)}</div><div class="fr-skeleton-table">${skeletonRows(5)}</div></section><section><div class="fr-skeleton-categories">${skeletonRows(2, "chip")}</div><div class="fr-skeleton-products">${skeletonRows(8, "product")}</div></section></div>`;
        }
        if (/catalogo-(paises|provincias|ciudades)/.test(id)) {
            return `<div class="fr-skeleton-catalog"><div class="fr-skeleton-heading"><i></i><span><b></b><b></b></span><i></i></div><div class="fr-skeleton-parent"><i></i><b></b></div><div class="fr-skeleton-list">${skeletonRows(8, "geo")}</div><div class="fr-skeleton-footer"><i></i><b></b></div></div>`;
        }
        if (id === "articulos") {
            return `<div class="fr-skeleton-document"><div class="fr-skeleton-top-grid"><section>${skeletonFields(8)}</section><section>${skeletonFields(6)}</section></div><section>${skeletonFields(5)}</section><div class="fr-skeleton-footer"><i></i><b></b></div></div>`;
        }
        if (/^catalogo-(marcas|unidades|departamentos|almacenes|suplidores|monedas|areas|cajas|cuentas-contables)$/.test(id)) {
            return `<div class="fr-skeleton-catalog"><div class="fr-skeleton-heading"><i></i><span><b></b><b></b></span><i></i></div><div class="fr-skeleton-fields">${skeletonFields(/suplidores|departamentos|almacenes|cajas/.test(id) ? 8 : 3)}</div><section class="fr-skeleton-table">${skeletonRows(6)}</section><div class="fr-skeleton-footer"><i></i><b></b></div></div>`;
        }
        if (/^(entradas-inventario|salidas-inventario|transferencia-inventario|asignar-departamentos)$/.test(id)) {
            return `<div class="fr-skeleton-document"><div class="fr-skeleton-top-grid"><section>${skeletonFields(6)}</section><section>${skeletonFields(4)}</section></div><section class="fr-skeleton-table">${skeletonRows(7)}</section><div class="fr-skeleton-footer"><i></i><b></b></div></div>`;
        }
        if (/roles|usuarios/.test(id)) {
            return `<div class="fr-skeleton-access"><div class="fr-skeleton-heading"><i></i><span><b></b><b></b></span><b></b></div><div class="fr-skeleton-toolbar"><b></b><i></i></div><div class="fr-skeleton-cards">${skeletonRows(6, "card")}</div></div>`;
        }
        if (
            /factura|cotizacion|devolucion|nota-debito|entrada|salida|transferencia|recibo|deposito|desembolso/.test(id) ||
            /invoice-window/.test(classes)
        ) {
            return `<div class="fr-skeleton-document"><div class="fr-skeleton-top-grid"><section>${skeletonFields(8)}</section><section>${skeletonFields(6)}</section><section>${skeletonFields(4)}</section></div><section class="fr-skeleton-table">${skeletonRows(7)}</section><div class="fr-skeleton-footer"><i></i><b></b><b></b></div></div>`;
        }
        if (
            /buscar-|busqueda|cola|anular|monitoreo/.test(id) ||
            /search-window|queue-window|void-window|ecf-monitor-window/.test(classes)
        ) {
            return `<div class="fr-skeleton-search"><div class="fr-skeleton-toolbar">${skeletonFields(3)}</div><section class="fr-skeleton-table">${skeletonRows(10)}</section><div class="fr-skeleton-footer"><i></i><i></i><i></i></div></div>`;
        }
        return `<div class="fr-skeleton-form"><div class="fr-skeleton-heading"><i></i><span><b></b><b></b></span></div><div class="fr-skeleton-fields">${skeletonFields(10)}</div><section class="fr-skeleton-table">${skeletonRows(5)}</section><div class="fr-skeleton-footer"><i></i><b></b></div></div>`;
    }

    function createInstance(target) {
        const host = target?.hasAttribute?.("data-stage")
            ? target.querySelector("[data-stage-body]") || target
            : target || document.body;
        const overlay = document.createElement("div");
        overlay.className = "fr-skeleton-overlay";
        overlay.hidden = true;
        overlay.setAttribute("aria-hidden", "true");
        overlay.innerHTML = `<div class="fr-skeleton-content">${viewMarkup(target)}</div>`;
        if (getComputedStyle(host).position === "static") host.style.position = "relative";
        host.appendChild(overlay);
        const instance = { host, overlay, count: 0, hideTimer: 0 };
        instances.set(target, instance);
        return instance;
    }

    function show(target = window.LuminisLoader?.resolveTarget?.()) {
        if (!target || !target.isConnected) target = document.body;
        const instance = instances.get(target) || createInstance(target);
        instance.count += 1;
        window.clearTimeout(instance.hideTimer);
        if (instance.overlay.hidden) {
            instance.overlay.hidden = false;
            instance.overlay.setAttribute("aria-hidden", "false");
            instance.overlay.classList.add("is-visible");
        }
        return instance;
    }

    function hide(instance) {
        if (!instance) return;
        instance.count = Math.max(0, instance.count - 1);
        if (instance.count) return;
        instance.hideTimer = window.setTimeout(() => {
            if (instance.count) return;
            instance.overlay.classList.remove("is-visible");
            instance.overlay.setAttribute("aria-hidden", "true");
            window.setTimeout(() => {
                if (!instance.count && instance.overlay.isConnected) instance.overlay.hidden = true;
            }, 180);
        }, 80);
    }

    window.LuminisSkeleton = Object.freeze({ show, hide });
})();
