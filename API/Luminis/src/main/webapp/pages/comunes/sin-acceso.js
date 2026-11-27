const S = (p) => `<svg viewBox="0 0 24 24" aria-hidden="true">${p}</svg>`;
const IC = {
    cuenta: S('<path d="M3 9.5 5 4h14l2 5.5M4 9.5V20h16V9.5M9 20v-5h6v5"/>'),
    usuario: S('<circle cx="12" cy="8" r="4"/><path d="M4 21c.8-4 3.8-6 8-6s7.2 2 8 6"/>'),
    acceso: S('<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>'),
    escudo: S('<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/><path d="m9 12 2 2 4-4"/>'),
    copiar: S('<rect x="9" y="9" width="11" height="11" rx="3"/><path d="M5 15H4V4h11v1"/>'),
    salir: S('<path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10"/>'),
    atras: S('<path d="M19 12H5M11 6l-6 6 6 6"/>'),
    sigue: S('<path d="M5 12h14M13 6l6 6-6 6"/>'),
    alerta: S('<path d="M12 3 2 21h20zM12 10v5M12 18h.01"/>'),
    llave: S('<circle cx="8" cy="9" r="4"/><path d="m11 12 9 9m-4-4 3-3m-6 0 3-3"/>'),
    correo: S('<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>'),
    sol: '<svg class="theme-icon-sun theme-sun" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/></svg>',
    luna: '<svg class="theme-icon-moon theme-moon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20 15a8 8 0 0 1-11-11 8 8 0 1 0 11 11Z"/></svg>'
};

const CSS = `
.sa { position: fixed; inset: 0; z-index: 100; container: sa / inline-size; color: var(--sa-ink); background: var(--sa-bg); font-family: var(--sa-fuente, inherit); }
.sa svg { fill: none; stroke: currentColor; stroke-width: 1.9; stroke-linecap: round; stroke-linejoin: round; }
.sa [hidden] { display: none !important; }
.sa-marco { display: grid; grid-template-columns: minmax(330px, 43%) minmax(0, 1fr); height: 100%; min-height: 0; }
.sa-historia { position: relative; display: flex; flex-direction: column; gap: 26px; min-height: 0; padding: clamp(26px, 3.6cqi, 52px); overflow: hidden auto; isolation: isolate; color: #f2f4f8; background: radial-gradient(ellipse at 35% 89%, color-mix(in srgb, var(--sa-acento) 45%, transparent), transparent 50%), radial-gradient(ellipse at 110% 2%, color-mix(in srgb, var(--sa-acento) 16%, transparent), transparent 45%), linear-gradient(142deg, var(--sa-h1), var(--sa-h2) 70%, var(--sa-h1)); scrollbar-width: none; }
.sa-historia::before { content: ""; position: absolute; z-index: -1; inset: 0; background: radial-gradient(#ffffff22 1px, transparent 1.4px) 0 0 / 30px 30px; -webkit-mask: linear-gradient(120deg, transparent 30%, #000); mask: linear-gradient(120deg, transparent 30%, #000); }
.sa-orbita { position: absolute; z-index: -1; right: -42%; bottom: -24%; width: 112%; aspect-ratio: 1; border: 1px solid color-mix(in srgb, var(--sa-acento) 30%, transparent); border-radius: 50%; background: radial-gradient(circle at 34% 36%, color-mix(in srgb, var(--sa-acento) 20%, transparent), transparent 62%); }
.sa-marca { display: flex; align-items: center; gap: 14px; }
.sa-marca img { width: 62px; height: 62px; object-fit: contain; filter: drop-shadow(0 10px 16px #0008); }
.sa-marca span { font: 700 .95rem var(--sa-titulo, inherit); color: #d6dbe6; }
.sa-centro { margin: auto 0; }
.sa-centro h1 { margin: 0; font: 800 clamp(2.1rem, 5.2cqi, 4.3rem)/1.02 var(--sa-titulo, inherit); letter-spacing: -.055em; text-wrap: balance; overflow-wrap: anywhere; }
.sa-centro h1.is-largo { font-size: clamp(1.7rem, 3.7cqi, 2.9rem); letter-spacing: -.045em; }
.sa-centro h1 em { color: var(--sa-acento-claro); font-style: normal; }
.sa-centro p { max-width: 34ch; margin: 16px 0 0; color: #d6dbe6; font-size: 1rem; line-height: 1.55; overflow-wrap: anywhere; }
.sa-datos { display: grid; grid-template-columns: 1fr 1fr; gap: 15px 20px; margin: 0; padding-top: 22px; border-top: 1px solid #ffffff24; }
.sa-datos > div { min-width: 0; }
.sa-datos .is-ancho { grid-column: 1 / -1; }
.sa-datos dt { color: #aeb6c6; font-size: .74rem; }
.sa-datos dd { margin: 2px 0 0; font-size: .9rem; font-weight: 600; line-height: 1.4; overflow-wrap: anywhere; }
.sa-datos button { display: inline-flex; align-items: center; gap: 8px; max-width: 100%; padding: 0; border: 0; color: inherit; background: none; font: inherit; text-align: left; cursor: pointer; }
.sa-datos button svg { width: 15px; height: 15px; flex: none; opacity: .6; }
.sa-datos button:hover svg { opacity: 1; }
.sa-aviso { display: flex; align-items: flex-start; gap: 10px; margin: 0; padding: 12px 14px; border: 1px solid #ffd27a55; border-radius: 14px; color: #ffe6b0; background: #ffb4241a; font-size: .8rem; line-height: 1.45; }
.sa-aviso svg { width: 18px; height: 18px; flex: none; margin-top: 1px; }
.sa-usuario { display: flex; align-items: center; gap: 8px; padding-top: 18px; border-top: 1px solid #ffffff24; }
.sa-perfil { display: flex; flex: 1; align-items: center; gap: 12px; min-width: 0; padding: 6px 12px 6px 6px; border: 0; border-radius: 16px; color: inherit; background: transparent; font: inherit; text-align: left; cursor: pointer; transition: background .15s; }
.sa-perfil:hover { background: #ffffff17; }
.sa-perfil > span { display: grid; width: 44px; height: 44px; flex: none; place-items: center; border-radius: 14px; color: #fff; background: linear-gradient(130deg, var(--sa-acento-claro), var(--sa-acento)); font: 800 .9rem var(--sa-titulo, inherit); }
.sa-perfil div { display: grid; min-width: 0; line-height: 1.3; }
.sa-perfil b, .sa-perfil small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sa-perfil b { font-size: .9rem; }
.sa-perfil small { color: #aeb6c6; font-size: .76rem; }
.sa-icono { display: grid; width: 44px; height: 44px; flex: none; place-items: center; padding: 0; border: 1px solid #ffffff26; border-radius: 14px; color: #eef1f6; background: #ffffff0f; cursor: pointer; transition: background .15s, color .15s, border-color .15s; }
.sa-icono svg { width: 19px; height: 19px; }
.sa-icono:hover { background: #ffffff1f; }
.sa-icono.is-salir:hover { color: #fff; border-color: #ff6b6b; background: #ff4d4d55; }
.sa .theme-icon-moon, .sa .theme-moon { display: none; }
[data-theme="dark"] .sa .theme-icon-sun, [data-theme="dark"] .sa .theme-sun { display: none; }
[data-theme="dark"] .sa .theme-icon-moon, [data-theme="dark"] .sa .theme-moon { display: block; }
.sa-panel { display: grid; grid-template-rows: auto minmax(0, 1fr) auto; min-width: 0; min-height: 0; }
.sa-pasos { display: flex; gap: 8px; padding: 24px clamp(20px, 4cqi, 58px) 0; overflow-x: auto; scrollbar-width: none; }
.sa-pasos button { display: grid; flex: 1 0 auto; gap: 10px; min-width: 84px; padding: 4px 0 8px; border: 0; color: var(--sa-muted); background: none; font-size: .8rem; font-weight: 600; text-align: left; cursor: pointer; transition: color .2s; }
.sa-pasos i { height: 4px; border-radius: 9px; background: color-mix(in srgb, var(--sa-acento) 17%, var(--sa-line)); transition: background .25s; }
.sa-pasos .is-actual i { background: linear-gradient(90deg, var(--sa-acento), var(--sa-acento-claro)); }
.sa-pasos span { display: flex; align-items: center; gap: 7px; }
.sa-pasos svg { width: 16px; height: 16px; }
.sa-pasos .is-actual, .sa-pasos button:hover { color: var(--sa-ink); }
.sa-pasos .is-actual svg { color: var(--sa-acento); }
.sa-form { min-height: 0; padding: 18px clamp(20px, 4cqi, 58px) 30px; display: grid; align-content: safe center; overflow-y: auto; }
.sa-form > section { min-width: 0; width: 100%; animation: sa-entra .35s cubic-bezier(.2, .8, .2, 1) both; }
@keyframes sa-entra { from { opacity: 0; transform: translateX(18px); } to { opacity: 1; transform: none; } }
.sa-cab { margin-bottom: 30px; text-align: center; }
.sa-cab h2 { margin: 0; font: 800 clamp(1.65rem, 3.4cqi, 2.5rem)/1.1 var(--sa-titulo, inherit); letter-spacing: -.045em; text-wrap: balance; }
.sa-cab p { margin: 8px 0 0; color: var(--sa-soft); font-size: .93rem; }
.sa-campos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.sa-campo { display: grid; gap: 6px; min-width: 0; }
.sa-campo.is-ancho { grid-column: 1 / -1; }
.sa-campo span { color: var(--sa-soft); font-size: .72rem; font-weight: 800; letter-spacing: .02em; }
.sa-campo b { display: block; min-height: 44px; padding: 11px 13px; border: 1px solid var(--sa-line); border-radius: 12px; color: var(--sa-ink); background: var(--sa-card); font-size: .88rem; font-weight: 500; overflow-wrap: anywhere; opacity: .92; }
.sa-filas { display: grid; gap: 10px; }
.sa-fila { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border: 1px solid var(--sa-line); border-radius: 18px; background: var(--sa-card); }
.sa-fila-ico { display: grid; width: 42px; height: 42px; flex: none; place-items: center; border-radius: 13px; color: var(--sa-acento); background: color-mix(in srgb, var(--sa-acento) 12%, transparent); }
.sa-fila-ico svg { width: 20px; height: 20px; }
.sa-fila > div { flex: 1; min-width: 0; }
.sa-fila b { display: block; font-size: .92rem; }
.sa-fila small { color: var(--sa-soft); font-size: .8rem; }
.sa-mini { flex: none; min-height: 36px; padding: 0 14px; border: 1px solid color-mix(in srgb, var(--sa-acento) 35%, var(--sa-line)); border-radius: 11px; color: var(--sa-acento); background: transparent; font-size: .78rem; font-weight: 800; cursor: pointer; }
.sa-mini:hover { background: color-mix(in srgb, var(--sa-acento) 10%, transparent); }
.sa-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.sa-chips em { padding: 4px 10px; border-radius: 999px; color: var(--sa-acento); background: color-mix(in srgb, var(--sa-acento) 12%, transparent); font-size: .74rem; font-style: normal; font-weight: 800; }
.sa-pie { display: flex; align-items: center; gap: 10px; padding: 14px clamp(20px, 4cqi, 58px); border-top: 1px solid var(--sa-line); }
.sa-pie p { flex: 1; margin: 0 4px; color: var(--sa-muted); font-size: .8rem; text-align: right; }
.sa-redondo { display: grid; width: 42px; height: 42px; place-items: center; padding: 0; border: 1px solid var(--sa-line); border-radius: 50%; color: var(--sa-ink); background: var(--sa-card); cursor: pointer; }
.sa-redondo:disabled { opacity: .35; cursor: default; }
.sa-redondo svg { width: 18px; height: 18px; }
.sa-vista { display: grid; grid-template-rows: auto minmax(0, 1fr); min-height: 0; }
.sa-volver { display: flex; align-items: center; gap: 10px; padding: 14px clamp(20px, 4cqi, 40px); border-bottom: 1px solid var(--sa-line); }
.sa-volver button { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; border: 1px solid var(--sa-line); border-radius: 12px; color: var(--sa-ink); background: var(--sa-card); font-size: .84rem; font-weight: 700; cursor: pointer; }
.sa-volver svg { width: 17px; height: 17px; }
.sa-vista > div:last-child { min-height: 0; padding: 18px clamp(16px, 3cqi, 36px); overflow-y: auto; }
.sa-esq { height: 44px; border-radius: 12px; background: linear-gradient(90deg, var(--sa-card), color-mix(in srgb, var(--sa-acento) 8%, var(--sa-card)), var(--sa-card)); background-size: 200% 100%; animation: sa-brillo 1.2s linear infinite; }
@keyframes sa-brillo { to { background-position: -200% 0; } }
@media (max-width: 900px) { .sa { overflow-y: auto; overscroll-behavior: contain; } }
@container sa (max-width: 900px) {
    .sa-marco { display: block; height: auto; }
    .sa-historia { gap: 18px; padding: 22px 18px; overflow: visible; }
    .sa-marca img { width: 48px; height: 48px; }
    .sa-centro { margin: 6px 0; }
    .sa-centro h1, .sa-centro h1.is-largo { font-size: clamp(1.7rem, 8cqi, 2.4rem); }
    .sa-centro p { margin-top: 8px; font-size: .9rem; }
    .sa-orbita { display: none; }
    .sa-panel, .sa-vista { min-height: 100dvh; }
    .sa-pasos { padding: 16px 16px 0; }
    .sa-form { overflow: visible; align-content: start; padding: 16px 16px 24px; }
    .sa-cab { margin-bottom: 18px; }
    .sa-cab h2 { font-size: 1.5rem; }
    .sa-pie { position: sticky; bottom: 0; padding: 12px 16px; background: var(--sa-bg); }
    .sa-volver { position: sticky; top: 0; z-index: 2; padding: 12px 16px; background: var(--sa-bg); }
    .sa-vista > div:last-child { overflow: visible; padding: 14px 12px; }
}
@container sa (max-width: 560px) {
    .sa-campos, .sa-datos { grid-template-columns: 1fr; }
    .sa-pasos button { min-width: 0; flex: 1 1 0; }
    .sa-pasos b { font-size: .74rem; }
    .sa-fila { flex-wrap: wrap; padding: 14px; }
    .sa-fila .sa-mini { width: 100%; }
    .sa-usuario { flex-wrap: wrap; }
}
`;

const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const fecha = (v) => {
    if (!v) return "—";
    const d = new Date(String(v).replace(" ", "T"));
    return Number.isNaN(d.getTime()) ? String(v) : d.toLocaleDateString("es-DO", { day: "2-digit", month: "short", year: "numeric" });
};

export async function montarSinAcceso({ sesion, marca, colores, etiquetaCuenta = "Cuenta", cargarCuenta, montarPantalla, salir, vistas = ["__perfil", "__seguridad"] }) {
    const vista = (clave) => !!montarPantalla && vistas.includes(clave);
    if (!document.getElementById("sa-estilos")) {
        const st = document.createElement("style");
        st.id = "sa-estilos";
        st.textContent = CSS;
        document.head.appendChild(st);
    }
    document.body.classList.add("sin-acceso");
    const raiz = document.createElement("div");
    raiz.className = "sa";
    Object.entries(colores || {}).forEach(([k, v]) => raiz.style.setProperty(`--sa-${k}`, v));
    document.body.appendChild(raiz);
    const perfil = sesion.perfil || {};
    const persona = [perfil.nombres, perfil.apellidos].filter(Boolean).join(" ") || sesion.usuario || "Usuario";
    const iniciales = persona.split(/\s+/).slice(0, 2).map((t) => t[0]?.toUpperCase() || "").join("");
    const admin = sesion.tipo === "Administracion";
    let c = {};
    let seccion = 0;
    const SECCIONES = [
        { icono: IC.cuenta, nombre: admin ? "Plataforma" : etiquetaCuenta, titulo: admin ? `Administración de ${marca.nombre}` : `Tu ${etiquetaCuenta.toLowerCase()} en ${marca.nombre}`, texto: admin ? "El área donde trabajas y su estado." : "Los datos de la cuenta a la que perteneces." },
        { icono: IC.usuario, nombre: "Mi usuario", titulo: "Así te ve el sistema", texto: "Tus datos de acceso y los roles que tienes asignados." },
        { icono: IC.acceso, nombre: "Acceso", titulo: "Aún no tienes opciones", texto: "Tu rol no tiene módulos, categorías ni opciones asignadas." }
    ];

    const campo = (etiqueta, valor, ancho = false) => `<label class="sa-campo${ancho ? " is-ancho" : ""}"><span>${esc(etiqueta)}</span><b>${esc(valor || "—")}</b></label>`;
    const fila = (ico, t, sub, extra = "") => `<div class="sa-fila"><span class="sa-fila-ico">${ico}</span><div><b>${t}</b><small>${sub}</small></div>${extra}</div>`;

    function secciones() {
        const u = c.usuario || {};
        const roles = c.roles || [];
        const tel = c.telefono ? `${c.prefijoTelefono || ""} ${c.telefono}`.trim() : "";
        const cuenta = admin
            ? `<div class="sa-campos">${campo("Plataforma", marca.nombre)}${campo("Área", "Administración")}${campo("Nombre", [c.nombres, c.apellidos].filter(Boolean).join(" "))}${campo("Correo", c.correo)}${campo("Usuario desde", fecha(c.fechaCreacion))}${campo("Estado", c.estado)}</div>`
            : `<div class="sa-campos">${campo("Nombre", c.nombreComercial, true)}${c.razonSocial !== undefined ? campo("Razón social", c.razonSocial) : ""}${campo(c.tipoCliente ? `Documento (${c.tipoCliente})` : "Documento", c.documento)}${campo("Correo", c.correo)}${campo("Teléfono", tel)}${c.whatsapp !== undefined ? campo("WhatsApp", c.whatsapp) : ""}${campo("Dirección", c.direccion, true)}${campo(`${etiquetaCuenta} desde`, fecha(c.fechaCreacion))}${campo("Estado", c.estado)}${c.empresas !== undefined ? campo("Empresas", c.empresas) : ""}${campo("Usuarios", c.usuarios)}</div>`;
        const yo = admin ? c : u;
        const usuario = `<div class="sa-campos">${campo("Usuario", yo.usuario || sesion.usuario)}${campo("Nombre", [yo.nombres, yo.apellidos].filter(Boolean).join(" ") || persona)}${campo("Correo", yo.correo)}${campo("Último acceso", fecha(yo.fechaUltimoAcceso))}${!admin ? campo("Propietario", yo.propietario ? "Sí" : "No") : ""}<label class="sa-campo is-ancho"><span>Roles asignados</span><b>${roles.length ? `<span class="sa-chips">${roles.map((r) => `<em>${esc(r)}</em>`).join("")}</span>` : "Sin roles"}</b></label></div>`;
        const acceso = `<div class="sa-filas">${fila(IC.alerta, "Sin opciones asignadas", "Tus roles no incluyen ningún módulo, categoría u opción del menú.")}${fila(IC.correo, "Pide acceso", "Un administrador puede asignarte un rol con las opciones que necesitas.")}${vista("__perfil") ? fila(IC.usuario, "Mi perfil", "Actualiza tus datos personales y de contacto.", '<button type="button" class="sa-mini" data-sa-abrir="__perfil">Abrir</button>') : ""}${vista("__seguridad") ? fila(IC.llave, "Seguridad", "Cambia tu contraseña y la verificación en dos pasos.", '<button type="button" class="sa-mini" data-sa-abrir="__seguridad">Abrir</button>') : ""}</div>`;
        return [cuenta, usuario, acceso];
    }

    function pintar() {
        const nombre = String((admin ? marca.nombre : c.nombreComercial) || marca.nombre).trim();
        const partes = nombre.split(/\s+/);
        const ultimo = partes.length > 1 ? partes.pop() : "";
        const contenido = secciones();
        const tel = c.telefono ? `${c.prefijoTelefono || ""} ${c.telefono}`.trim() : "";
        raiz.innerHTML = `<div class="sa-marco">
            <aside class="sa-historia"><div class="sa-orbita" aria-hidden="true"></div>
                <div class="sa-marca"><img src="${esc(marca.logo)}" alt=""><span>${admin ? "Administración" : `Mi ${esc(etiquetaCuenta.toLowerCase())}`}</span></div>
                <div class="sa-centro"><h1 class="${nombre.length > 26 ? "is-largo" : ""}">${esc(partes.join(" "))}${ultimo ? ` <em>${esc(ultimo)}</em>` : ""}</h1><p>${admin ? "Área administrativa de la plataforma." : esc(c.razonSocial || c.correo || "")}</p></div>
                <dl class="sa-datos">${admin
                    ? `<div><dt>Usuario</dt><dd>${esc(sesion.usuario)}</dd></div><div><dt>Roles</dt><dd>${esc((c.roles || []).join(", ") || "Sin roles")}</dd></div><div class="is-ancho"><dt>Correo</dt><dd>${esc(c.correo || "—")}</dd></div>`
                    : `<div><dt>Documento</dt><dd>${c.documento ? `<button type="button" data-sa-copiar="${esc(c.documento)}" title="Copiar"><b>${esc(c.documento)}</b>${IC.copiar}</button>` : "—"}</dd></div><div><dt>Teléfono</dt><dd>${esc(tel || "—")}</dd></div><div class="is-ancho"><dt>Correo</dt><dd>${esc(c.correo || "—")}</dd></div><div class="is-ancho"><dt>Dirección</dt><dd>${esc(c.direccion || "—")}</dd></div>`}</dl>
                <p class="sa-aviso">${IC.escudo}<span>Tu usuario no tiene opciones asignadas. Solo puedes ver esta información; pide a un administrador que te asigne un rol.</span></p>
                <footer class="sa-usuario"><button type="button" class="sa-perfil" ${vista("__perfil") ? 'data-sa-abrir="__perfil"' : vista("__seguridad") ? 'data-sa-abrir="__seguridad"' : ""} title="Mi cuenta"><span>${esc(iniciales)}</span><div><b>${esc(persona)}</b><small>${esc(sesion.usuario || "")}</small></div></button><button type="button" class="sa-icono" data-theme-toggle aria-label="Cambiar tema" title="Cambiar tema">${IC.sol}${IC.luna}</button><button type="button" class="sa-icono is-salir" data-sa-salir aria-label="Cerrar sesión" title="Cerrar sesión">${IC.salir}</button></footer>
            </aside>
            <div class="sa-panel" data-sa-panel>
                <nav class="sa-pasos" role="tablist">${SECCIONES.map((x, i) => `<button type="button" role="tab" data-sa-ir="${i}" class="${i === seccion ? "is-actual" : ""}" aria-selected="${i === seccion}"><i></i><span>${x.icono}<b>${esc(x.nombre)}</b></span></button>`).join("")}</nav>
                <div class="sa-form"><section><header class="sa-cab"><h2>${esc(SECCIONES[seccion].titulo)}</h2><p>${esc(SECCIONES[seccion].texto)}</p></header>${contenido[seccion]}</section></div>
                <footer class="sa-pie"><p>Solo lectura</p><button type="button" class="sa-redondo" data-sa-mover="-1" aria-label="Anterior"${seccion === 0 ? " disabled" : ""}>${IC.atras}</button><button type="button" class="sa-redondo" data-sa-mover="1" aria-label="Siguiente"${seccion === SECCIONES.length - 1 ? " disabled" : ""}>${IC.sigue}</button></footer>
            </div></div>`;
    }

    async function abrirVista(clave) {
        const panel = raiz.querySelector("[data-sa-panel]");
        const titulo = clave === "__seguridad" ? "Seguridad" : "Mi perfil";
        panel.className = "sa-vista";
        panel.innerHTML = `<div class="sa-volver"><button type="button" data-sa-volver>${IC.atras}<span>Volver</span></button><b>${titulo}</b></div><div><div class="sa-esq"></div></div>`;
        try { await montarPantalla(clave, panel.lastElementChild); }
        catch (e) { panel.lastElementChild.innerHTML = `<p>${esc(e.message)}</p>`; }
    }

    raiz.addEventListener("click", async (e) => {
        const ir = e.target.closest("[data-sa-ir]");
        if (ir) { seccion = Number(ir.dataset.saIr); pintar(); return; }
        const mv = e.target.closest("[data-sa-mover]");
        if (mv && !mv.disabled) { seccion = Math.max(0, Math.min(SECCIONES.length - 1, seccion + Number(mv.dataset.saMover))); pintar(); return; }
        if (e.target.closest("[data-sa-volver]")) { pintar(); return; }
        const ab = e.target.closest("[data-sa-abrir]");
        if (ab && montarPantalla) { abrirVista(ab.dataset.saAbrir); return; }
        const cp = e.target.closest("[data-sa-copiar]");
        if (cp) { try { await navigator.clipboard.writeText(cp.dataset.saCopiar); } catch (_) {} return; }
        if (e.target.closest("[data-sa-salir]")) salir?.();
    });

    raiz.innerHTML = `<div class="sa-marco"><aside class="sa-historia"><div class="sa-esq" style="height:62px;width:62px"></div><div class="sa-centro"><div class="sa-esq" style="height:70px"></div></div></aside><div class="sa-panel"><div></div><div class="sa-form">${'<div class="sa-esq" style="margin-bottom:12px"></div>'.repeat(5)}</div><div></div></div></div>`;
    try { c = (await cargarCuenta()) || {}; } catch (_) { c = {}; }
    pintar();
}
