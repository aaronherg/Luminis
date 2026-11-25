let ui;
let raiz;
let datos = null;

const base = () => (ui?.sesion?.tipo === "Administracion" ? "/api/administracion/usuarios" : "/api/clientes/usuarios");
const soloDigitos = valor => String(valor || "").replace(/\D/g, "");
const S = p => `<svg viewBox="0 0 24 24" aria-hidden="true">${p}</svg>`;
const ICO = {
    id: '<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="12" r="2.5"/><path d="M14 10h4M14 14h3"/>',
    tel: '<path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2"/>',
    mapa: '<path d="M12 21s7-6.2 7-12a7 7 0 1 0-14 0c0 5.8 7 12 7 12Z"/><circle cx="12" cy="9" r="2.2"/>',
    escudo: '<path d="M12 3 20 6v5c0 5-3.4 8.3-8 10-4.6-1.7-8-5-8-10V6z"/><path d="m8.5 12 2.3 2.3 4.8-5"/>',
    usuario: '<circle cx="12" cy="8" r="4"/><path d="M4 21c.8-4 3.8-6 8-6s7.2 2 8 6"/>',
    correo: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>'
};

export function loadStyles() {
    if (document.getElementById("lm-perfil-style")) return;
    const estilo = document.createElement("style");
    estilo.id = "lm-perfil-style";
    estilo.textContent = `
.pf { display: grid; gap: 18px; }
.pf-portada { position: relative; overflow: hidden; border: 1px solid var(--line); border-radius: 26px; background: var(--surface); }
.pf-portada__fondo { height: 120px; background: radial-gradient(circle at 15% 20%, color-mix(in srgb, var(--accent) 45%, transparent), transparent 55%), radial-gradient(circle at 85% 0%, color-mix(in srgb, var(--accent-2, var(--accent)) 40%, transparent), transparent 50%), linear-gradient(120deg, var(--surface-alt), var(--bg)); }
.pf-portada__fondo::after { content: ""; position: absolute; inset: 0 0 auto; height: 120px; background-image: linear-gradient(color-mix(in srgb, var(--accent) 9%, transparent) 1px, transparent 1px), linear-gradient(90deg, color-mix(in srgb, var(--accent) 9%, transparent) 1px, transparent 1px); background-size: 28px 28px; }
.pf-portada__contenido { position: relative; display: flex; flex-wrap: wrap; align-items: flex-end; gap: 20px; margin-top: -48px; padding: 0 26px 24px; }
.pf-avatar { display: grid; width: 104px; height: 104px; flex: none; place-items: center; border: 4px solid var(--surface); border-radius: 30px; color: #fff; background: linear-gradient(135deg, var(--accent), var(--accent-strong, var(--accent))); box-shadow: 0 16px 34px color-mix(in srgb, var(--accent) 45%, transparent); font: 800 2.2rem var(--font-display); }
.pf-identidad { display: grid; flex: 1; gap: 8px; min-width: 220px; }
.pf-identidad h1 { margin: 0; font: 800 clamp(1.5rem, 2.6vw, 2rem) var(--font-display); letter-spacing: -.03em; }
.pf-identidad p { margin: 0; color: var(--text-secondary); }
.pf-mono { font-family: var(--font-mono); }
.pf-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.pf-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border: 1px solid var(--line); border-radius: 99px; background: var(--bg); font-size: .72rem; font-weight: 700; }
.pf-chip svg { width: 13px; height: 13px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.pf-chip.is-ok { border-color: color-mix(in srgb, var(--success) 50%, transparent); color: var(--success); }
.pf-chip.is-alerta { border-color: color-mix(in srgb, var(--warning) 50%, transparent); color: var(--warning); }
.pf-metricas { display: flex; flex-wrap: wrap; gap: 10px; }
.pf-metricas div { display: grid; gap: 2px; min-width: 110px; padding: 12px 16px; border: 1px solid var(--line); border-radius: 16px; background: var(--bg); }
.pf-metricas b { font: 800 1.15rem var(--font-display); }
.pf-metricas small { color: var(--muted); font-size: .74rem; }
.pf-cuerpo { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; align-items: start; }
.pf-form { display: grid; gap: 16px; }
.pf-card { border: 1px solid var(--line); border-radius: 18px; background: var(--surface); padding: 20px; }
.pf-card-head { margin: 0 0 14px; }
.pf-grid { display: grid; gap: 14px; }
.pf-h { display: flex; align-items: center; gap: 10px; margin: 0; font: 800 .98rem var(--font-display); }
.pf-h svg { width: 18px; height: 18px; color: var(--accent); fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.pf-acciones { position: sticky; bottom: 12px; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 4px; padding: 12px 16px; border: 1px solid var(--line); border-radius: 18px; background: color-mix(in srgb, var(--surface) 92%, transparent); backdrop-filter: blur(12px); }
.pf-acciones-info { display: flex; align-items: center; gap: 12px; min-width: 0; }
.pf-acciones-ico { display: grid; width: 38px; height: 38px; flex: none; place-items: center; border-radius: 12px; color: var(--accent); background: var(--surface-alt); }
.pf-acciones-ico svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.pf-acciones-info > div { display: grid; min-width: 0; line-height: 1.3; }
.pf-acciones-info strong { font: 800 .84rem var(--font-display); }
.pf-acciones-info small { color: var(--muted); font-size: .74rem; }
.pf-lado { position: sticky; top: 0; display: grid; gap: 16px; }
.pf-text { margin: 0 0 12px; color: var(--text-secondary); font-size: .9rem; }
.pf-datos { display: grid; gap: 8px; }
.pf-datos div { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--line); font-size: .86rem; }
.pf-datos div:last-child { border-bottom: 0; }
.pf-datos dt { color: var(--muted); }
.pf-datos dd { margin: 0; font-weight: 700; text-align: right; }
.pf-progreso header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.pf-progreso header h3 { margin: 0; font: 800 .98rem var(--font-display); }
.pf-progreso header b { color: var(--accent); font: 800 1rem var(--font-display); }
.pf-progreso i { display: block; height: 8px; overflow: hidden; border-radius: 8px; background: var(--surface-alt); }
.pf-progreso i::before { content: ""; display: block; width: var(--v, 0%); height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--accent-2, var(--accent)), var(--accent)); }
.pf-switch { display: flex; align-items: flex-start; gap: 12px; padding: 12px 14px; border: 1px solid var(--line); border-radius: 14px; background: var(--bg); cursor: pointer; }
.pf-switch input { margin-top: 3px; }
.pf-switch span { display: grid; gap: 2px; }
.pf-switch small { color: var(--muted); font-size: .76rem; }
.pf-row { display: grid; grid-template-columns: 110px 1fr; gap: 10px; }
@media (max-width: 980px) { .pf-cuerpo { grid-template-columns: 1fr; } .pf-lado { position: static; } }`;
    document.head.appendChild(estilo);
}

export function render() {
    return `<div class="pf" data-perfil><div class="fr-skeleton" style="height:190px;border-radius:22px"></div>
        <div class="pf-cuerpo"><div class="fr-skeleton" style="height:420px;border-radius:18px"></div><div class="fr-skeleton" style="height:420px;border-radius:18px"></div></div></div>`;
}

function pintar() {
    const d = datos || {};
    const nombre = `${d.nombres || ""} ${d.apellidos || ""}`.trim() || d.usuario || "Usuario";
    const iniciales = nombre.split(/\s+/).slice(0, 2).map(p => p[0]?.toUpperCase() || "").join("");
    const completos = [d.nombres, d.apellidos, d.documento, d.correo, d.telefono, d.direccion].filter(x => x && String(x).trim()).length;
    const porcentaje = Math.round(completos / 6 * 100);
    const desde = d.fechaCreacion ? new Date(d.fechaCreacion).toLocaleDateString("es-DO", { day: "2-digit", month: "short", year: "numeric" }) : "—";
    const roles = d.rolesNombres || [];
    const whatsappIgual = !!d.whatsapp && soloDigitos(d.whatsapp) === soloDigitos(d.prefijoTelefono) + soloDigitos(d.telefono);
    const activo = (d.estado || "Activo") === "Activo";
    raiz.innerHTML = `<div class="pf">
        <section class="pf-portada"><div class="pf-portada__fondo"></div><div class="pf-portada__contenido">
            <span class="pf-avatar">${ui.escapar(iniciales)}</span>
            <div class="pf-identidad"><h1>${ui.escapar(nombre)}</h1><p><span class="pf-mono">${ui.escapar(d.usuario || "")}</span> · ${ui.escapar(d.propietario ? "Propietario" : roles[0] || "Usuario")}</p>
                <div class="pf-chips"><span class="pf-chip ${activo ? "is-ok" : "is-alerta"}">${ui.escapar(d.estado || "Activo")}</span>${d.correo ? `<span class="pf-chip">${S(ICO.correo)}${ui.escapar(d.correo)}</span>` : ""}</div></div>
            <div class="pf-metricas"><div><b>${roles.length}</b><small>Roles asignados</small></div><div><b>${ui.escapar(desde)}</b><small>Miembro desde</small></div><div><b>${porcentaje}%</b><small>Perfil completo</small></div></div>
        </div></section>
        <div class="pf-cuerpo">
            <form class="pf-form" data-form novalidate>
                <section class="pf-card"><header class="pf-card-head"><h3 class="pf-h">${S(ICO.id)}Datos personales</h3></header><div class="pf-grid">
                    ${ui.campo({ nombre: "nombres", etiqueta: "Nombres", requerido: true, valor: d.nombres, max: 120 })}
                    ${ui.campo({ nombre: "apellidos", etiqueta: "Apellidos", valor: d.apellidos, max: 120 })}
                    ${ui.campo({ nombre: "documento", etiqueta: "Cédula o RNC", valor: d.documento, max: 11, atributos: 'inputmode="numeric"' })}
                </div></section>
                <section class="pf-card"><header class="pf-card-head"><h3 class="pf-h">${S(ICO.tel)}Contacto</h3></header><div class="pf-grid">
                    ${ui.campo({ nombre: "correo", etiqueta: "Correo", tipo: "email", requerido: true, valor: d.correo, max: 160, clase: "is-full", ayuda: "Aquí recibirás los avisos de tu cuenta." })}
                    <div class="pf-row">
                        <label class="fr-field"><span>Prefijo</span><input class="fr-input" name="prefijoTelefono" data-phone-prefix value="${ui.escapar(d.prefijoTelefono || "+1")}" aria-label="Prefijo del país"></label>
                        <label class="fr-field"><span>Teléfono</span><input class="fr-input" name="telefono" type="tel" inputmode="tel" maxlength="20" placeholder="809 000 0000" value="${ui.escapar(d.telefono || "")}"></label>
                    </div>
                    <label class="fr-switch-row is-full"><span><b>Recibir avisos por WhatsApp en este número</b><small>Te avisaremos de novedades de tu cuenta.</small></span><label class="fr-switch"><input type="checkbox" name="mismoWhatsapp" ${whatsappIgual ? "checked" : ""}><span></span></label></label>
                </div></section>
                <section class="pf-card"><header class="pf-card-head"><h3 class="pf-h">${S(ICO.mapa)}Dirección</h3></header><div class="pf-grid">
                    ${ui.campo({ nombre: "direccion", etiqueta: "Dirección", tipo: "textarea", valor: d.direccion, max: 250, clase: "is-full" })}
                </div></section>
                <div class="pf-acciones"><div class="pf-acciones-info"><span class="pf-acciones-ico"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 3h12l3 3v15H5zM8 3v6h9V3M8 21v-8h9v8"/></svg></span><div><strong>Acciones</strong><small>Revisa tus datos personales y de contacto antes de guardar.</small></div></div><button class="fr-btn fr-btn-primary" type="submit">${ui.icono("save")}<span>Guardar cambios</span></button></div>
            </form>
            <aside class="pf-lado">
                <section class="pf-card"><header class="pf-card-head"><h3 class="pf-h">${S(ICO.escudo)}Seguridad</h3></header><p class="pf-text">Usa una contraseña única con mayúscula, minúscula, número y carácter especial.</p><button class="fr-btn" type="button" data-ir-seguridad style="width:100%">${ui.icono("lock")}<span>Ir a Seguridad</span></button></section>
                <section class="pf-card"><header class="pf-card-head"><h3 class="pf-h">${S(ICO.usuario)}Tu cuenta</h3></header><dl class="pf-datos">
                    <div><dt>Usuario</dt><dd>${ui.escapar(d.usuario || "—")}</dd></div>
                    <div><dt>Rol</dt><dd>${ui.escapar(roles.join(", ") || "—")}</dd></div>
                    <div><dt>Tipo</dt><dd>${d.propietario ? "Propietario" : "Usuario"}</dd></div>
                </dl></section>
                <section class="pf-card pf-progreso"><header><h3>Perfil completo</h3><b>${porcentaje}%</b></header><i style="--v:${porcentaje}%"></i><p class="pf-text" style="margin-top:12px">${porcentaje < 100 ? "Completa tus datos para que podamos contactarte sin problemas." : "¡Todo listo! Tus datos están completos."}</p></section>
            </aside>
        </div>
    </div>`;
    window.LuminisTelefono?.mejorar(raiz);
}

async function cargar() {
    try {
        datos = await LuminisApi.post(`${base()}/getActual`, {});
        pintar();
    } catch (error) {
        raiz.innerHTML = `<div class="fr-empty">${ui.icono("ban")}<p>No se pudo cargar tu perfil. ${ui.escapar(error.message || "")}</p></div>`;
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    raiz.addEventListener("click", event => {
        if (event.target.closest("[data-ir-seguridad]")) {
            ui.abrirSeguridad ? ui.abrirSeguridad() : document.querySelector("[data-change-password]")?.click();
        }
    });
    raiz.addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.target.closest("[data-form]");
        if (!form) return;
        const boton = form.querySelector('button[type="submit"]');
        await ui.ocupado(boton, async () => {
            try {
                ui.exigir(form, [
                    ["nombres", "Los nombres son obligatorios."],
                    ["correo", "Ingresa un correo válido.", valor => ui.correoValido(valor)],
                    ["documento", "La cédula debe tener 11 dígitos o el RNC 9.", valor => !valor || /^(\d{9}|\d{11})$/.test(soloDigitos(valor))],
                    ["telefono", "Escribe un número de teléfono válido.", valor => !valor || /^[\d\s()-]{7,20}$/.test(valor)]
                ]);
                const v = ui.valores(form);
                const numero = soloDigitos(v.telefono);
                if (v.mismoWhatsapp && !numero) { AppNotifications.warning("Escribe tu teléfono para recibir avisos por WhatsApp."); return; }
                datos = await LuminisApi.put(`${base()}/updateActual`, {
                    nombres: v.nombres, apellidos: v.apellidos, documento: soloDigitos(v.documento) || null, correo: v.correo,
                    prefijoTelefono: numero ? v.prefijoTelefono || "+1" : null, telefono: numero || null,
                    whatsapp: v.mismoWhatsapp && numero ? (v.prefijoTelefono || "+1") + numero : null, direccion: v.direccion
                });
                AppNotifications.success("Perfil actualizado correctamente");
                pintar();
            } catch (error) {
                ui.notificarError(error);
            }
        });
    });
    await cargar();
}

export function skeleton() {
    const b = (css) => `<div class="fr-skeleton" style="${css}"></div>`;
    const campo = `<div style="display:grid;gap:7px">${b("height:12px;width:120px")}${b("height:44px;border-radius:12px")}</div>`;
    const card = (n) => `<section class="pf-card"><div style="display:flex;gap:10px;align-items:center;margin-bottom:14px">${b("height:18px;width:18px;border-radius:6px")}${b("height:15px;width:150px")}</div><div class="pf-grid">${Array(n).fill(campo).join("")}</div></section>`;
    const lado = `<section class="pf-card"><div style="display:flex;gap:10px;align-items:center;margin-bottom:12px">${b("height:18px;width:18px;border-radius:6px")}${b("height:15px;width:120px")}</div><div style="display:grid;gap:10px">${b("height:12px;width:92%")}${b("height:38px;border-radius:12px")}</div></section>`;
    return `<div class="pf">
        <section class="pf-portada" style="min-height:auto"><div class="pf-portada__fondo"></div><div class="pf-portada__contenido">${b("height:104px;width:104px;border-radius:30px;flex:none")}<div style="display:grid;gap:12px;flex:1;min-width:220px">${b("height:28px;width:45%")}${b("height:14px;width:60%")}</div><div style="display:flex;gap:10px">${b("height:60px;width:110px;border-radius:16px")}${b("height:60px;width:110px;border-radius:16px")}${b("height:60px;width:110px;border-radius:16px")}</div></div></section>
        <div class="pf-cuerpo"><div style="display:grid;gap:16px">${card(3)}${card(3)}${card(1)}</div><aside class="pf-lado">${lado}${lado}${lado}</aside></div>
    </div>`;
}

export function reset() {
    datos = null;
}
