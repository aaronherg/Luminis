let ui;
let raiz;
let estado2fa = { activo: false, correo: null, tieneCorreo: true, servicioDisponible: true };

const A2FA = "/api/seguridad/autenticacion/dosfactores";
const REGLAS = [["8 caracteres o más", v => v.length >= 8], ["Una mayúscula", v => /[A-Z]/.test(v)], ["Una minúscula", v => /[a-z]/.test(v)], ["Un número", v => /\d/.test(v)], ["Un carácter especial", v => /[^A-Za-z0-9\s]/.test(v)]];
const S = p => `<svg viewBox="0 0 24 24" aria-hidden="true">${p}</svg>`;
const ICO = {
    escudo: '<path d="M12 3 20 6v5c0 5-3.4 8.3-8 10-4.6-1.7-8-5-8-10V6z"/><path d="m8.5 12 2.3 2.3 4.8-5"/>',
    llave: '<circle cx="8" cy="15" r="4"/><path d="m10.8 12.2 8.2-8.2M17 5l2 2M15 7l2 2"/>',
    monitor: '<rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/>',
    bombilla: '<path d="M9 18h6M10 22h4M12 2a7 7 0 0 0-4 12.7c.6.5 1 1.3 1 2.1V17h6v-.2c0-.8.4-1.6 1-2.1A7 7 0 0 0 12 2Z"/>',
    salir: '<path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10"/>'
};

export function loadStyles() {
    if (document.getElementById("fr-seguridad-style")) return;
    const e = document.createElement("style");
    e.id = "fr-seguridad-style";
    e.textContent = `
.sg { display: grid; gap: 18px; }
.sg-cab h1 { margin: 0; font: 800 clamp(1.4rem,2.4vw,1.8rem) var(--font-display); }
.sg-cab p { margin: 6px 0 0; color: var(--text-secondary); max-width: 640px; }
.sg-cuerpo { display: grid; grid-template-columns: minmax(0,1fr) 320px; gap: 18px; align-items: start; }
.sg-col { display: grid; gap: 18px; min-width: 0; }
.sg-card { border: 1px solid var(--line); border-radius: 18px; background: var(--surface); padding: 20px; }
.sg-h { display: flex; align-items: center; gap: 10px; margin: 0 0 6px; font: 800 .98rem var(--font-display); }
.sg-h svg { width: 18px; height: 18px; color: var(--accent); fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.sg-sub { margin: 0 0 14px; color: var(--text-secondary); font-size: .88rem; }
.sg-grid { display: grid; gap: 14px; }
.sg-reglas { list-style: none; margin: -4px 0 12px; padding: 0; display: grid; gap: 6px; }
.sg-reglas li { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: .8rem; }
.sg-reglas li i { width: 8px; height: 8px; border-radius: 50%; background: var(--line-strong, var(--line)); flex: none; transition: background .15s; }
.sg-reglas li.is-ok { color: var(--text); }
.sg-reglas li.is-ok i { background: var(--success); }
.sg-2fa { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.sg-2fa > div { min-width: 0; }
.sg-2fa b { font-size: .92rem; }
.sg-2fa small { display: block; margin-top: 3px; color: var(--muted); font-size: .8rem; }
.sg-switch { position: relative; flex: none; width: 46px; height: 26px; }
.sg-switch input { position: absolute; opacity: 0; width: 100%; height: 100%; margin: 0; cursor: pointer; }
.sg-switch span { position: absolute; inset: 0; border-radius: 999px; background: var(--surface-alt); border: 1px solid var(--line); transition: background .15s; }
.sg-switch span::after { content: ""; position: absolute; top: 3px; left: 3px; width: 18px; height: 18px; border-radius: 50%; background: var(--text-secondary); transition: transform .15s, background .15s; }
.sg-switch input:checked + span { background: var(--accent); border-color: var(--accent); }
.sg-switch input:checked + span::after { transform: translateX(20px); background: #fff; }
.sg-switch input:disabled { cursor: default; }
.sg-tips { margin: 0; padding-left: 18px; display: grid; gap: 8px; color: var(--text-secondary); font-size: .86rem; }
.sg-lado { display: grid; gap: 16px; }
.sg-datos { display: grid; gap: 8px; }
.sg-datos div { display: flex; justify-content: space-between; gap: 12px; padding: 7px 0; border-bottom: 1px solid var(--line); font-size: .85rem; }
.sg-datos div:last-child { border-bottom: 0; }
.sg-datos dt { color: var(--muted); }
.sg-datos dd { margin: 0; font-weight: 700; text-align: right; }
.sg-acciones { position: sticky; bottom: 12px; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 12px 16px; border: 1px solid var(--line); border-radius: 18px; background: color-mix(in srgb, var(--surface) 92%, transparent); backdrop-filter: blur(12px); }
.sg-acciones-info { display: flex; align-items: center; gap: 12px; min-width: 0; }
.sg-acciones-ico { display: grid; width: 38px; height: 38px; flex: none; place-items: center; border-radius: 12px; color: var(--accent); background: var(--surface-alt); }
.sg-acciones-ico svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.sg-acciones-info > div { display: grid; min-width: 0; line-height: 1.3; }
.sg-acciones-info strong { font: 800 .84rem var(--font-display); }
.sg-acciones-info small { color: var(--muted); font-size: .74rem; }
.sg-cajas { display: flex; gap: 9px; justify-content: center; margin: 18px 0 8px; flex-wrap: wrap; }
.sg-caja { width: 44px; height: 54px; text-align: center; font-size: 1.4rem; font-weight: 800; text-transform: uppercase; border-radius: 12px; border: 1.5px solid var(--line); background: var(--bg); color: var(--text); outline: none; transition: border-color .15s, box-shadow .15s; }
.sg-caja:focus { border-color: var(--accent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 22%, transparent); }
.sg-link { background: none; border: 0; color: var(--accent); cursor: pointer; font: inherit; text-decoration: underline; padding: 0; }
@media (max-width: 980px) { .sg-cuerpo { grid-template-columns: 1fr; } }`;
    document.head.appendChild(e);
}

export function render() {
    return `<div class="sg">
        <div class="sg-cuerpo">
            <div class="sg-col">
                <section class="sg-card"><h3 class="sg-h">${S(ICO.llave)}Cambiar contraseña</h3><p class="sg-sub">Te pediremos la contraseña actual para confirmar que eres tú.</p>
                    <form class="sg-grid" data-form novalidate>
                        ${campo("actual", "Contraseña actual")}
                        ${campo("nueva", "Nueva contraseña")}
                        <ul class="sg-reglas" data-reglas>${REGLAS.map(([t]) => `<li><i></i>${t}</li>`).join("")}</ul>
                        ${campo("confirmacion", "Confirmar contraseña")}
                    </form>
                </section>
                <section class="sg-card"><h3 class="sg-h">${S(ICO.escudo)}Verificación de acceso personal</h3><p class="sg-sub">Añade una segunda capa de seguridad: además de tu contraseña, un código enviado a tu correo.</p>
                    <div class="sg-2fa"><div><b>Autenticación de dos factores</b><small data-2fa-estado>Cargando…</small></div>
                        <label class="sg-switch"><input type="checkbox" name="dosFactores" disabled><span></span></label>
                    </div>
                </section>
            </div>
            <aside class="sg-lado">
                <section class="sg-card"><h3 class="sg-h">${S(ICO.monitor)}Sesión actual</h3><dl class="sg-datos" data-sesion></dl></section>
                <section class="sg-card"><h3 class="sg-h">${S(ICO.bombilla)}Recomendaciones</h3><ul class="sg-tips"><li>Usa una contraseña única que no uses en otros sitios.</li><li>Activa la verificación en dos pasos.</li><li>Cierra sesión cuando uses un equipo compartido.</li></ul></section>
                <section class="sg-card"><h3 class="sg-h">${S(ICO.salir)}Cerrar sesión</h3><p class="sg-sub">Sal de tu cuenta en este dispositivo.</p><button class="fr-btn" type="button" data-salir style="width:100%">${S(ICO.salir)}<span>Cerrar sesión</span></button></section>
            </aside>
        </div>
        <div class="sg-acciones"><div class="sg-acciones-info"><span class="sg-acciones-ico">${S(ICO.escudo)}</span><div><strong>Acciones</strong><small>Tu nueva contraseña reemplazará la actual al guardar.</small></div></div>
            <button class="fr-btn fr-btn-primary" type="button" data-guardar>${S(ICO.llave)}<span>Actualizar contraseña</span></button></div>
    </div>`;
}

function campo(nombre, etiqueta) {
    return `<label class="fr-field"><span>${etiqueta}</span><input class="fr-input" type="password" name="${nombre}" maxlength="120" autocomplete="${nombre === "actual" ? "current-password" : "new-password"}"></label>`;
}

function pintar2fa(stage) {
    const sw = stage.querySelector("[name=dosFactores]");
    const txt = stage.querySelector("[data-2fa-estado]");
    sw.checked = estado2fa.activo;
    if (!estado2fa.tieneCorreo) { sw.disabled = true; txt.textContent = "Agrega un correo en tu perfil para poder activar la verificación en dos pasos."; return; }
    sw.disabled = false;
    if (estado2fa.activo) txt.textContent = `Activa. Enviaremos el código a ${estado2fa.correo || "tu correo"} cada vez que inicies sesión.`;
    else if (!estado2fa.servicioDisponible) txt.textContent = "Para activarla necesitas un servicio de correo disponible (SMTP).";
    else txt.textContent = "Desactivada. Actívala para pedir un código al iniciar sesión.";
}

function cajasHtml() {
    const uno = '<input inputmode="text" maxlength="1" autocomplete="one-time-code" aria-label="Carácter del código" class="sg-caja">';
    return `<div class="sg-cajas" data-cajas>${uno.repeat(6)}</div>`;
}
function enlazarCajas(cont, alCompletar) {
    const cajas = [...cont.querySelectorAll("[data-cajas] input")];
    cajas.forEach((c, i) => {
        c.addEventListener("input", () => { c.value = c.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(-1); if (c.value && cajas[i + 1]) cajas[i + 1].focus(); if (cajas.every(x => x.value)) alCompletar(); });
        c.addEventListener("keydown", e => { if (e.key === "Backspace" && !c.value && cajas[i - 1]) cajas[i - 1].focus(); else if (e.key === "Enter") { e.preventDefault(); alCompletar(); } });
        c.addEventListener("paste", e => { const t = (e.clipboardData?.getData("text") || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6); if (!t) return; e.preventDefault(); t.split("").forEach((d, j) => { if (cajas[j]) cajas[j].value = d; }); (cajas[t.length] || cajas[5]).focus(); if (t.length === 6) alCompletar(); });
    });
    setTimeout(() => cajas[0]?.focus(), 60);
    return () => cajas.map(c => c.value).join("");
}

function activar() {
    return new Promise(resolve => {
        (async () => {
            let correo = "tu correo";
            try { const r = await LuminisApi.post(`${A2FA}/activar/enviar`, {}); correo = r?.correo || "tu correo"; }
            catch (err) { ui.notificarError(err); resolve(false); return; }
            const { raiz: capa, cerrar } = ui.modal({
                titulo: "Verificación de acceso personal", ancho: "sm",
                cuerpo: `<p style="margin:0 0 4px;color:var(--text-secondary);font-size:.9rem">Enviamos un código de 6 caracteres a <strong>${ui.escapar(correo)}</strong>. Escríbelo para activar la verificación en dos pasos.</p>
                    ${cajasHtml()}
                    <p style="text-align:center;font-size:.85rem;color:var(--muted)">¿No te llegó? <button type="button" class="sg-link" data-reenviar>Reenviar código</button></p>
                    <div class="fr-form-actions" style="justify-content:flex-end;margin-top:8px"><button class="fr-btn" type="button" data-cancelar>Cancelar</button><button class="fr-btn fr-btn-primary" type="button" data-ok>Activar</button></div>`
            });
            let resuelto = false;
            const leer = enlazarCajas(capa, () => capa.querySelector("[data-ok]")?.click());
            const confirmar = () => ui.ocupado(capa.querySelector("[data-ok]"), async () => {
                const cod = leer();
                if (cod.length !== 6) { AppNotifications.warning("Escribe el código completo de 6 caracteres."); return; }
                try { await LuminisApi.post(`${A2FA}/activar/confirmar`, { codigo: cod }); }
                catch (err) { ui.notificarError(err); capa.querySelectorAll("[data-cajas] input").forEach(c => (c.value = "")); capa.querySelector("[data-cajas] input")?.focus(); return; }
                resuelto = true; cerrar(); AppNotifications.success("Verificación en dos pasos activada."); resolve(true);
            });
            capa.querySelector("[data-ok]").addEventListener("click", confirmar);
            capa.querySelector("[data-reenviar]").addEventListener("click", e => ui.ocupado(e.currentTarget, async () => {
                try { const r = await LuminisApi.post(`${A2FA}/activar/enviar`, {}); AppNotifications.success(`Código reenviado a ${r?.correo || "tu correo"}.`); } catch (err) { ui.notificarError(err); }
            }));
            capa.querySelector("[data-cancelar]").addEventListener("click", () => { cerrar(); if (!resuelto) resolve(false); });
        })();
    });
}

function desactivar() {
    return new Promise(resolve => {
        const { raiz: capa, cerrar } = ui.modal({
            titulo: "Desactivar verificación", ancho: "sm",
            cuerpo: `<p style="margin:0 0 12px;color:var(--text-secondary);font-size:.9rem">Para desactivar la autenticación de dos factores, escribe tu contraseña actual.</p>
                <label class="fr-field is-full"><span>Tu contraseña</span><input class="fr-input" type="password" name="contrasena" maxlength="120" autocomplete="current-password"></label>
                <div class="fr-form-actions" style="justify-content:flex-end;margin-top:12px"><button class="fr-btn" type="button" data-cancelar>Cancelar</button><button class="fr-btn fr-btn-danger" type="button" data-ok>Desactivar</button></div>`
        });
        let resuelto = false;
        const input = capa.querySelector("[name=contrasena]");
        const confirmar = () => ui.ocupado(capa.querySelector("[data-ok]"), async () => {
            if (!input.value) { AppNotifications.warning("Escribe tu contraseña."); input.focus(); return; }
            try { await LuminisApi.put(`${A2FA}/desactivar`, { contrasena: input.value }); }
            catch (err) { ui.notificarError(err); input.focus(); input.select(); return; }
            resuelto = true; cerrar(); AppNotifications.success("Verificación en dos pasos desactivada."); resolve(true);
        });
        capa.querySelector("[data-ok]").addEventListener("click", confirmar);
        input.addEventListener("keydown", e => { if (e.key === "Enter") { e.preventDefault(); confirmar(); } });
        capa.querySelector("[data-cancelar]").addEventListener("click", () => { cerrar(); if (!resuelto) resolve(false); });
        setTimeout(() => input.focus(), 60);
    });
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const sesion = api.sesion || {};
    const perfil = sesion.perfil || {};
    stage.querySelector("[data-sesion]").innerHTML = [
        ["Usuario", sesion.usuario || "—"],
        ["Nombre", [perfil.nombres, perfil.apellidos].filter(Boolean).join(" ") || "—"],
        ["Correo", perfil.correo || "—"],
        ["Dispositivo", navigator.userAgentData?.platform || navigator.platform || "—"]
    ].map(([k, v]) => `<div><dt>${k}</dt><dd>${ui.escapar(v)}</dd></div>`).join("");

    const form = stage.querySelector("[data-form]");
    const reglas = [...stage.querySelectorAll("[data-reglas] li")];
    form.nueva.addEventListener("input", () => reglas.forEach((li, i) => li.classList.toggle("is-ok", REGLAS[i][1](form.nueva.value))));

    (async () => { try { estado2fa = await LuminisApi.post(`${A2FA}/estado`, {}); } catch (_) { } pintar2fa(stage); })();

    const sw = stage.querySelector("[name=dosFactores]");
    sw.addEventListener("change", async () => {
        if (sw.checked) { sw.checked = false; sw.disabled = true; const ok = await activar(); estado2fa.activo = ok; sw.disabled = false; pintar2fa(stage); }
        else { sw.checked = true; sw.disabled = true; const ok = await desactivar(); estado2fa.activo = !ok; sw.disabled = false; pintar2fa(stage); }
    });

    stage.querySelector("[data-salir]").addEventListener("click", () => document.querySelector("[data-logout]")?.click());

    stage.querySelector("[data-guardar]").addEventListener("click", event => ui.ocupado(event.currentTarget, async () => {
        try {
            ui.exigir(form, [
                ["actual", "Escribe tu contraseña actual."],
                ["nueva", "La contraseña necesita 8 caracteres con mayúscula, minúscula, número y carácter especial.", v => REGLAS.every(([, f]) => f(v))],
                ["confirmacion", "Las contraseñas no coinciden.", (v, c) => v === form.nueva.value]
            ]);
            if (form.actual.value === form.nueva.value) { AppNotifications.warning("La nueva contraseña debe ser distinta a la actual."); return; }
            await LuminisApi.put("/api/seguridad/autenticacion/cambiarContrasena", { contrasenaActual: form.actual.value, contrasenaNueva: form.nueva.value });
            form.reset();
            reglas.forEach(li => li.classList.remove("is-ok"));
            AppNotifications.success("Contraseña actualizada correctamente.");
        } catch (error) { ui.notificarError(error); }
    }));
}

export function reset() {
    estado2fa = { activo: false, correo: null, tieneCorreo: true, servicioDisponible: true };
}
