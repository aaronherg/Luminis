let socket = null;
let ui;
let caja;
let total = 0;

const PLANTILLAS = {
    "sensor.lectura": { tipo: "sensor.lectura", temp: 23.4, hum: 61 },
    "pedido.estado": { tipo: "pedido.estado", pedido: "A-901", estado: "en_camino" },
    "gps.posicion": { tipo: "gps.posicion", lat: 18.4861, lng: -69.9312 },
    "alerta.nueva": { tipo: "alerta.nueva", nivel: "alta", mensaje: "Temperatura fuera de rango" },
    "chat.mensaje": { tipo: "chat.mensaje", texto: "hola" }
};

const CIERRES = {
    1000: "Cierre normal",
    1006: "Conexión perdida",
    4400: "Usuario inválido",
    4401: "API key no válida",
    4402: "Sin plan vigente",
    4403: "Sin permiso para conectar",
    4429: "Límite de conexiones alcanzado"
};

export function render() {
    return `
        <div class="console-shell" data-consola>
            <aside class="console-side">
                <section class="fr-panel">
                    <header class="fr-panel-head"><div><h2>Conexión</h2><p>Abre un socket real con una de tus API keys</p></div></header>
                    <div class="fr-panel-body">
                        <form class="fr-form" data-conectar novalidate>
                            <label class="fr-field is-full"><span>API key</span><input class="fr-input fr-mono" name="clave" type="password" placeholder="lm_…" autocomplete="off" required></label>
                            <label class="fr-field is-full"><span>Usuario o dispositivo</span><input class="fr-input" name="usuario" maxlength="80" placeholder="sensor-01" required></label>
                            <div class="fr-form-actions is-full">
                                <button class="fr-btn" type="button" data-desconectar disabled>Desconectar</button>
                                <button class="fr-btn fr-btn-gold" type="submit" data-boton-conectar>Conectar</button>
                            </div>
                        </form>
                    </div>
                </section>
                <div class="console-status" data-estado>
                    <span class="lm-pulse-dot is-off"></span>
                    <div><strong data-estado-texto>Desconectado</strong><small data-estado-detalle>Sin sesión abierta</small></div>
                </div>
                <section class="fr-panel">
                    <header class="fr-panel-head"><div><h2>Límites de la sesión</h2><p>Recibidos en el evento ready</p></div></header>
                    <div class="fr-panel-body"><dl class="fr-kv" data-limites><div><dt>Estado</dt><dd>Conéctate para verlos</dd></div></dl></div>
                </section>
            </aside>
            <section class="console-stream">
                <div class="console-stream-bar">
                    <span>eventos · <b data-contador>0</b></span>
                    <button class="fr-btn fr-btn-sm" type="button" data-limpiar>Limpiar</button>
                </div>
                <ol class="console-log" data-registro><li class="console-empty" data-vacio>Conecta un dispositivo y publica un evento para verlo viajar aquí en tiempo real.</li></ol>
                <form class="console-compose" data-enviar novalidate>
                    <div class="console-presets">${Object.keys(PLANTILLAS).map(nombre => `<button type="button" data-plantilla="${nombre}">${nombre}</button>`).join("")}</div>
                    <div class="console-compose-row">
                        <label class="fr-field"><span>Destino</span><input class="fr-input" name="destino" maxlength="80" placeholder="dashboard" disabled></label>
                        <label class="fr-field"><span>Evento (JSON o texto)</span><textarea class="fr-textarea" name="mensaje" placeholder='{"temp": 23.4}' disabled></textarea></label>
                        <div class="fr-form-actions">
                            <button class="fr-btn" type="button" data-ping disabled>Ping</button>
                            <button class="fr-btn fr-btn-primary" type="submit" data-boton-enviar disabled>Publicar</button>
                        </div>
                    </div>
                </form>
            </section>
        </div>`;
}

function registrar(direccion, tipo, datos, clase = "") {
    if (!caja) return;
    const lista = caja.querySelector("[data-registro]");
    caja.querySelector("[data-vacio]")?.remove();
    const item = document.createElement("li");
    item.className = clase || (direccion === "→" ? "is-out" : direccion === "←" ? "is-in" : "is-sys");
    const hora = document.createElement("time");
    hora.textContent = new Date().toLocaleTimeString("es-DO", { hour12: false });
    const flecha = document.createElement("span");
    flecha.className = "dir";
    flecha.textContent = direccion;
    const cuerpo = document.createElement("div");
    const etiqueta = document.createElement("span");
    etiqueta.className = "tipo";
    etiqueta.textContent = tipo;
    const pre = document.createElement("pre");
    pre.textContent = typeof datos === "string" ? datos : JSON.stringify(datos, null, 2);
    cuerpo.append(etiqueta, pre);
    if (tipo === "message" && datos?.mensajeId && datos?.origen) {
        const visto = document.createElement("button");
        visto.type = "button";
        visto.className = "fr-btn fr-btn-sm";
        visto.textContent = "Confirmar procesado";
        visto.addEventListener("click", () => {
            enviarTrama({ type: "seen", mensajeId: datos.mensajeId, destino: datos.origen });
            visto.disabled = true;
        });
        cuerpo.append(visto);
    }
    item.append(hora, flecha, cuerpo);
    lista.append(item);
    while (lista.children.length > 200) lista.firstElementChild.remove();
    lista.scrollTop = lista.scrollHeight;
    total += 1;
    caja.querySelector("[data-contador]").textContent = ui.formato.numero(total);
}

function estado(texto, detalle, modo) {
    if (!caja) return;
    const panel = caja.querySelector("[data-estado]");
    panel.classList.toggle("is-connecting", modo === "conectando");
    panel.querySelector(".lm-pulse-dot").classList.toggle("is-off", modo !== "conectado" && modo !== "conectando");
    caja.querySelector("[data-estado-texto]").textContent = texto;
    caja.querySelector("[data-estado-detalle]").textContent = detalle;
}

function habilitar(conectado) {
    if (!caja) return;
    caja.querySelectorAll('[data-enviar] input, [data-enviar] textarea, [data-ping], [data-boton-enviar]').forEach(control => (control.disabled = !conectado));
    caja.querySelector("[data-desconectar]").disabled = !conectado;
    caja.querySelector("[data-boton-conectar]").disabled = conectado;
}

function enviarTrama(trama) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        AppNotifications.warning("Primero conecta el socket.");
        return;
    }
    socket.send(JSON.stringify(trama));
    registrar("→", trama.type || "message", trama);
}

function pintarLimites(limites = {}) {
    const valor = (dato, unidad = "") => (dato == null ? "∞" : `${ui.formato.numero(dato)}${unidad}`);
    const filas = [
        ["Plan", ui.escapar(limites.plan || "—")],
        ["Peticiones al mes", valor(limites.peticionesMes)],
        ["Conexiones", valor(limites.conexiones)],
        ["Tamaño por evento", valor(limites.mensajeKb, " KB")],
        ["Cola offline", limites.retencionSegundos == null ? "—" : `${ui.formato.numero(limites.retencionSegundos)} s`]
    ];
    caja.querySelector("[data-limites]").innerHTML = filas.map(([titulo, dato]) => `<div><dt>${titulo}</dt><dd>${dato}</dd></div>`).join("");
}

function conectar(clave, usuario) {
    const protocolo = location.protocol === "https:" ? "wss://" : "ws://";
    const base = window.LuminisConfig?.contextPath || "";
    estado("Conectando…", usuario, "conectando");
    const actual = new WebSocket(`${protocolo}${location.host}${base}/socket?apikey=${encodeURIComponent(clave)}&usuario=${encodeURIComponent(usuario)}`);
    socket = actual;
    actual.onopen = () => {
        if (socket !== actual) return;
        registrar("•", "open", `Socket abierto como "${usuario}"`);
    };
    actual.onmessage = mensaje => {
        if (socket !== actual) return;
        let datos;
        try {
            datos = JSON.parse(mensaje.data);
        } catch (_) {
            datos = mensaje.data;
        }
        const tipo = datos?.type || "texto";
        if (tipo === "ready") {
            estado("Conectado", `${usuario} · sesión ${String(datos.sesion || "").slice(0, 8)}`, "conectado");
            habilitar(true);
            pintarLimites(datos.limites || {});
        }
        registrar("←", tipo, datos, tipo === "error" ? "is-error" : "is-in");
    };
    actual.onerror = () => {
        if (socket !== actual) return;
        registrar("•", "error", "Error de conexión", "is-error");
    };
    actual.onclose = evento => {
        if (socket !== actual) return;
        socket = null;
        const motivo = CIERRES[evento.code] || evento.reason || "Conexión cerrada";
        estado("Desconectado", `${evento.code} · ${motivo}`, "cerrado");
        habilitar(false);
        registrar("•", "close", { code: evento.code, motivo: evento.reason || motivo }, evento.code === 1000 ? "is-sys" : "is-error");
    };
}

export function bind(stage, api) {
    ui = api;
    caja = stage.querySelector("[data-consola]");
    total = 0;
    const formConexion = caja.querySelector("[data-conectar]");
    const formEnvio = caja.querySelector("[data-enviar]");
    formConexion.addEventListener("submit", evento => {
        evento.preventDefault();
        try {
            ui.exigir(formConexion, [
                ["clave", "La API key debe comenzar con lm_.", valor => /^lm_[A-Za-z0-9_-]+$/.test(valor.trim())],
                ["usuario", "Usa de 1 a 80 caracteres: letras, números, punto, guion, guion bajo, @ o dos puntos.", valor => /^[A-Za-z0-9._@:-]{1,80}$/.test(valor.trim())]
            ]);
        } catch (error) {
            ui.notificarError(error);
            return;
        }
        socket?.close(1000, "Reconexión");
        conectar(formConexion.elements.clave.value.trim(), formConexion.elements.usuario.value.trim());
    });
    caja.querySelector("[data-desconectar]").addEventListener("click", () => socket?.close(1000, "Cierre solicitado"));
    caja.querySelector("[data-ping]").addEventListener("click", () => enviarTrama({ type: "ping", ref: crypto.randomUUID() }));
    caja.querySelector("[data-limpiar]").addEventListener("click", () => {
        total = 0;
        caja.querySelector("[data-contador]").textContent = "0";
        caja.querySelector("[data-registro]").innerHTML = '<li class="console-empty" data-vacio>Registro limpio. Los nuevos eventos aparecerán aquí.</li>';
    });
    caja.querySelectorAll("[data-plantilla]").forEach(boton => boton.addEventListener("click", () => {
        formEnvio.elements.mensaje.value = JSON.stringify(PLANTILLAS[boton.dataset.plantilla], null, 2);
        formEnvio.elements.mensaje.focus();
    }));
    formEnvio.addEventListener("submit", evento => {
        evento.preventDefault();
        const destino = formEnvio.elements.destino.value.trim();
        const texto = formEnvio.elements.mensaje.value.trim();
        if (!destino || !texto) {
            AppNotifications.error("Completa el destino y el evento a publicar.");
            return;
        }
        let mensaje = texto;
        try {
            mensaje = JSON.parse(texto);
        } catch (_) {
            mensaje = texto;
        }
        enviarTrama({ type: "message", destino, mensaje, ref: crypto.randomUUID() });
    });
}

export function reset() {
    const actual = socket;
    socket = null;
    actual?.close(1000, "Cambio de pantalla");
    caja = null;
    ui = null;
    total = 0;
}

export function skeleton() {
    return window.LuminisSk.lista({ filtros: 2, columnas: 6 });
}
