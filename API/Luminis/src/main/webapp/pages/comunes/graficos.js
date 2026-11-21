const PALETA = ["#7aa5ff", "#ffc94a", "#4ee6ff", "#b28cff", "#3ee2a4", "#ff9f5a", "#ff6b81", "#8fa2d6"];
let secuencia = 0;

const escapar = valor =>
    String(valor ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

export function color(indice) {
    return PALETA[indice % PALETA.length];
}

export function area({ puntos, formato = v => v, etiqueta = p => p.etiqueta, secundaria = null, alto = 220 }) {
    const id = `g${++secuencia}`;
    if (!puntos.length) return `<div class="fr-chart" data-chart="${id}"></div>`;
    const ancho = 640;
    const margen = { arriba: 16, abajo: 26, izquierda: 8, derecha: 8 };
    const valores = puntos.map(p => Number(p.valor || 0));
    const maximo = Math.max(1, ...valores, ...(secundaria ? puntos.map(p => Number(p.secundario || 0)) : []));
    const util = ancho - margen.izquierda - margen.derecha;
    const x = i => margen.izquierda + (puntos.length === 1 ? util / 2 : (i / (puntos.length - 1)) * util);
    const y = v => margen.arriba + (1 - v / maximo) * (alto - margen.arriba - margen.abajo);
    const linea = serie =>
        serie
            .map((v, i) => {
                if (i === 0) return `M${x(i)},${y(v)}`;
                const xp = (x(i - 1) + x(i)) / 2;
                return `C${xp},${y(serie[i - 1])} ${xp},${y(v)} ${x(i)},${y(v)}`;
            })
            .join(" ");
    const trazo = linea(valores);
    const relleno = `${trazo} L${x(valores.length - 1)},${alto - margen.abajo} L${x(0)},${alto - margen.abajo} Z`;
    const guias = [0.25, 0.5, 0.75, 1]
        .map(f => `<line class="eje" x1="0" x2="${ancho}" y1="${y(maximo * f)}" y2="${y(maximo * f)}"/>`)
        .join("");
    const paso = Math.max(1, Math.ceil(puntos.length / 7));
    const etiquetas = puntos
        .map((p, i) => (i % paso === 0 || i === puntos.length - 1 ? `<text class="eje-texto" x="${x(i)}" y="${alto - 6}" text-anchor="middle">${escapar(etiqueta(p))}</text>` : ""))
        .join("");
    const zonas = puntos
        .map((p, i) => {
            const izquierda = i === 0 ? 0 : (x(i - 1) + x(i)) / 2;
            const derecha = i === puntos.length - 1 ? ancho : (x(i) + x(i + 1)) / 2;
            return `<rect class="zona" data-i="${i}" x="${izquierda}" y="0" width="${derecha - izquierda}" height="${alto}" fill="transparent"/><circle class="punto" cx="${x(i)}" cy="${y(valores[i])}" r="4.5"/>`;
        })
        .join("");
    return `
        <div class="fr-chart" data-chart="${id}" data-puntos='${escapar(JSON.stringify(puntos.map(p => ({ e: etiqueta(p), v: formato(p.valor), s: secundaria ? secundaria.formato(p.secundario) : null }))))}' data-secundaria="${secundaria ? escapar(secundaria.nombre) : ""}">
            <svg viewBox="0 0 ${ancho} ${alto}" role="img">
                <defs>
                    <linearGradient id="${id}-fill" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0%" style="stop-color:var(--accent-strong);stop-opacity:0.42"/>
                        <stop offset="100%" style="stop-color:var(--accent-strong);stop-opacity:0"/>
                    </linearGradient>
                </defs>
                ${guias}
                <path d="${relleno}" fill="url(#${id}-fill)"/>
                ${secundaria ? `<path class="linea-2" d="${linea(puntos.map(p => Number(p.secundario || 0)))}"/>` : ""}
                <path class="linea" d="${trazo}"/>
                ${etiquetas}
                ${zonas}
            </svg>
        </div>`;
}

export function activarTooltips(raiz) {
    raiz.querySelectorAll("[data-chart]").forEach(grafico => {
        if (grafico.dataset.activo) return;
        grafico.dataset.activo = "true";
        let datos = [];
        try {
            datos = JSON.parse(grafico.dataset.puntos || "[]");
        } catch (_) {
            datos = [];
        }
        const tip = document.createElement("div");
        tip.className = "fr-chart-tip";
        tip.hidden = true;
        grafico.appendChild(tip);
        grafico.addEventListener("mousemove", evento => {
            const zona = evento.target.closest(".zona");
            grafico.querySelectorAll(".punto.is-visible").forEach(p => p.classList.remove("is-visible"));
            if (!zona) return void (tip.hidden = true);
            const punto = zona.nextElementSibling;
            punto.classList.add("is-visible");
            const dato = datos[Number(zona.dataset.i)];
            const caja = grafico.getBoundingClientRect();
            const circulo = punto.getBoundingClientRect();
            tip.innerHTML = `<span>${escapar(dato?.e)}</span><strong>${escapar(dato?.v)}</strong>${dato?.s != null ? `<span>${escapar(grafico.dataset.secundaria)}: ${escapar(dato.s)}</span>` : ""}`;
            tip.style.left = `${circulo.left - caja.left + circulo.width / 2}px`;
            tip.style.top = `${circulo.top - caja.top}px`;
            tip.hidden = false;
        });
        grafico.addEventListener("mouseleave", () => {
            tip.hidden = true;
            grafico.querySelectorAll(".punto.is-visible").forEach(p => p.classList.remove("is-visible"));
        });
    });
}

export function dona({ items, formato = v => v, centro = "" }) {
    const total = items.reduce((t, i) => t + Number(i.valor || 0), 0);
    const radio = 60;
    const circunferencia = 2 * Math.PI * radio;
    let acumulado = 0;
    const segmentos = total
        ? items
              .map((item, i) => {
                  const fraccion = Number(item.valor || 0) / total;
                  const largo = Math.max(0, fraccion * circunferencia - 2);
                  const segmento = `<circle r="${radio}" cx="75" cy="75" stroke="${color(i)}" stroke-dasharray="${largo} ${circunferencia}" stroke-dashoffset="${-acumulado}" style="filter:drop-shadow(0 0 6px ${color(i)}66)"/>`;
                  acumulado += fraccion * circunferencia;
                  return segmento;
              })
              .join("")
        : `<circle r="${radio}" cx="75" cy="75" stroke="var(--surface-alt)"/>`;
    return `
        <div class="fr-donut">
            <div class="fr-donut-center"><svg viewBox="0 0 150 150"><circle r="${radio}" cx="75" cy="75" stroke="var(--surface-alt)"/>${segmentos}</svg><strong>${escapar(centro)}</strong></div>
            <ul>${items
                .map(
                    (item, i) =>
                        `<li><i style="background:${color(i)};box-shadow:0 0 8px ${color(i)}"></i><span data-i18n-skip>${escapar(item.etiqueta)}</span><b>${escapar(formato(item.valor))}</b></li>`
                )
                .join("")}</ul>
        </div>`;
}

export function barras({ items, formato = v => v }) {
    const maximo = Math.max(1, ...items.map(i => Number(i.valor || 0)));
    return `<div class="fr-hbar">${items
        .map(
            item => `<div class="fr-hbar-row"><span data-i18n-skip title="${escapar(item.etiqueta)}">${escapar(item.etiqueta)}</span><b>${escapar(formato(item.valor))}</b><div class="fr-meter"><span style="width:${(Number(item.valor || 0) / maximo) * 100}%"></span></div></div>`
        )
        .join("")}</div>`;
}

export function diasEntre(desde, hasta) {
    const lista = [];
    const inicio = new Date(`${desde}T00:00:00`);
    const fin = new Date(`${hasta}T00:00:00`);
    for (let d = new Date(inicio); d <= fin; d.setDate(d.getDate() + 1)) {
        lista.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`);
    }
    return lista;
}

export function fechaCorta(iso) {
    const fecha = new Date(`${String(iso).slice(0, 10)}T00:00:00`);
    return fecha.toLocaleDateString("es-DO", { day: "2-digit", month: "short" });
}

export function hoyIso(diasAtras = 0) {
    const d = new Date();
    d.setDate(d.getDate() - diasAtras);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export function filtrosFecha({ desde, hasta, extra = "" }) {
    return `
        <form class="fr-filtros" data-filtros>
            <label class="fr-field"><span>Desde</span><input class="fr-input" type="date" name="desde" value="${desde}" required></label>
            <label class="fr-field"><span>Hasta</span><input class="fr-input" type="date" name="hasta" value="${hasta}" required></label>
            <div class="fr-rangos">
                <button class="fr-btn fr-btn-sm" type="button" data-rango="6">7 días</button>
                <button class="fr-btn fr-btn-sm" type="button" data-rango="29">30 días</button>
                <button class="fr-btn fr-btn-sm" type="button" data-rango="89">90 días</button>
                <button class="fr-btn fr-btn-sm" type="button" data-rango="364">1 año</button>
            </div>
            ${extra}
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn" type="button" data-exportar>${LuminisPanel.icono("download")}<span>Exportar CSV</span></button>
            <button class="fr-btn fr-btn-primary" type="submit">${LuminisPanel.icono("refresh")}<span>Generar</span></button>
        </form>`;
}

export function enlazarFiltros(raiz, alGenerar, alExportar) {
    const form = raiz.querySelector("[data-filtros]");
    form.addEventListener("submit", evento => {
        evento.preventDefault();
        alGenerar({ desde: form.elements.desde?.value || null, hasta: form.elements.hasta?.value || null, buscar: form.elements.buscar?.value?.trim() || null });
    });
    form.addEventListener("click", evento => {
        const rango = evento.target.closest("[data-rango]");
        if (rango) {
            form.elements.desde.value = hoyIso(Number(rango.dataset.rango));
            form.elements.hasta.value = hoyIso(0);
            form.requestSubmit();
        }
        if (evento.target.closest("[data-exportar]")) alExportar();
    });
}

export function exportarCsv(nombre, columnas, filas) {
    const celda = v => {
        const texto = String(v ?? "");
        return /[",;\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto;
    };
    const contenido = [columnas.map(c => celda(c.titulo)).join(","), ...filas.map(f => columnas.map(c => celda(c.valor(f))).join(","))].join("\n");
    const enlace = document.createElement("a");
    enlace.href = URL.createObjectURL(new Blob(["﻿" + contenido], { type: "text/csv;charset=utf-8" }));
    enlace.download = `${nombre}.csv`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.setTimeout(() => URL.revokeObjectURL(enlace.href), 2000);
}
