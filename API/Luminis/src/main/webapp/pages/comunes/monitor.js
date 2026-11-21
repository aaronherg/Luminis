export function crearMonitor({ administracion = false } = {}) {
    let ui;
    let caja;
    let timer;
    let pausado = false;
    let filas = [];
    let ultimaCarga = null;

    function render() {
        return `
            <div class="monitor" data-monitor>
                <div class="fr-stats" data-stats></div>
                <div class="fr-toolbar">
                    <label class="fr-search"><svg class="fr-svg" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg><input class="fr-input" type="search" placeholder="Filtrar por usuario, API key o IP" data-filtro></label>
                    <span class="monitor-live" data-estado><span class="lm-pulse-dot"></span>En vivo · cada 5 s</span>
                    <span class="fr-toolbar-spacer"></span>
                    <button class="fr-btn" type="button" data-pausa>Pausar</button>
                    <button class="fr-btn fr-btn-primary" type="button" data-actualizar>Actualizar</button>
                </div>
                <section class="fr-panel">
                    <header class="fr-panel-head"><div><h2>Sesiones abiertas</h2><p data-resumen>Conexiones WebSocket activas en este momento</p></div></header>
                    <div data-lista></div>
                </section>
            </div>`;
    }

    function relativo(valor) {
        const fecha = new Date(valor);
        if (Number.isNaN(fecha.getTime())) return "—";
        const segundos = Math.max(0, Math.round((Date.now() - fecha.getTime()) / 1000));
        if (segundos < 60) return `hace ${segundos} s`;
        if (segundos < 3600) return `hace ${Math.round(segundos / 60)} min`;
        if (segundos < 86400) return `hace ${Math.round(segundos / 3600)} h`;
        return ui.formato.fechaHora(valor);
    }

    function pintar() {
        if (!caja) return;
        const texto = caja.querySelector("[data-filtro]").value.trim().toLowerCase();
        const visibles = texto ? filas.filter(s => [s.usuario, s.apiKey, s.ip, s.clienteUuid].join(" ").toLowerCase().includes(texto)) : filas;
        const columnas = [
            { titulo: "Usuario / dispositivo", valor: s => `<span class="fr-cell-main"><span class="lm-pulse-dot"></span>${ui.escapar(s.usuario)}</span><span class="fr-cell-sub fr-mono">${ui.escapar(String(s.sesion).slice(0, 12))}</span>` }
        ];
        if (administracion) {
            columnas.push({ titulo: "Cliente", valor: s => `<span class="fr-mono">${ui.escapar(String(s.clienteUuid || s.clienteId || "—").slice(0, 8))}</span>` });
        }
        columnas.push(
            { titulo: "API key", valor: s => ui.escapar(s.apiKey || "—") },
            { titulo: "IP", valor: s => `<span class="fr-mono">${ui.escapar(s.ip || "—")}</span>` },
            { titulo: "Conectado", valor: s => `<span class="fr-cell-main">${relativo(s.conectado)}</span><span class="fr-cell-sub">${ui.formato.fechaHora(s.conectado)}</span>` },
            { titulo: "Enviados", clase: "is-right", valor: s => `<b>${ui.formato.numero(s.enviados)}</b>` },
            { titulo: "Recibidos", clase: "is-right", valor: s => `<b>${ui.formato.numero(s.recibidos)}</b>` },
            { titulo: "Última actividad", valor: s => relativo(s.ultimaActividad) },
            {
                titulo: "Acciones",
                clase: "is-actions",
                valor: () => ui.puede("monitor", "desconectar")
                    ? ui.botonAccion("cerrar-sesion", "unplug", "Cerrar esta sesión", "is-danger") + ui.botonAccion("cerrar-usuario", "ban", "Desconectar todas las sesiones del usuario", "is-danger")
                    : ""
            }
        );
        caja.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: visibles,
            clave: "sesion",
            vacio: texto ? "Ninguna sesión coincide con el filtro." : "No hay conexiones activas en este momento.",
            columnas
        });
    }

    async function cargar() {
        try {
            const respuesta = await MonitorService.listar();
            if (!caja) return;
            const datos = respuesta?.data ?? respuesta ?? {};
            filas = datos.sesiones || [];
            ultimaCarga = new Date();
            const tarjetas = [
                { titulo: "Conexiones abiertas", valor: ui.formato.numero(datos.conexiones), iconoNombre: "plug", tono: "is-gold" },
                { titulo: "Usuarios en línea", valor: ui.formato.numero(datos.usuarios), iconoNombre: "users", tono: "is-cyan" },
                { titulo: "Eventos en cola offline", valor: ui.formato.numero(datos.enCola), iconoNombre: "clock" }
            ];
            if (administracion) tarjetas.splice(2, 0, { titulo: "Clientes conectados", valor: ui.formato.numero(datos.clientes), iconoNombre: "building-2", tono: "is-success" });
            caja.querySelector("[data-stats]").innerHTML = tarjetas.map(ui.tarjetaDato).join("");
            caja.querySelector("[data-resumen]").textContent = `Actualizado a las ${ultimaCarga.toLocaleTimeString("es-DO")}`;
            pintar();
        } catch (error) {
            ui?.notificarError(error);
        }
    }

    async function bind(stage, api) {
        ui = api;
        caja = stage.querySelector("[data-monitor]");
        caja.addEventListener("input", evento => {
            if (evento.target.matches("[data-filtro]")) pintar();
        });
        caja.addEventListener("click", async evento => {
            if (evento.target.closest("[data-actualizar]")) {
                await ui.ocupado(evento.target.closest("[data-actualizar]"), cargar);
                return;
            }
            const pausa = evento.target.closest("[data-pausa]");
            if (pausa) {
                pausado = !pausado;
                pausa.textContent = pausado ? "Reanudar" : "Pausar";
                caja.querySelector("[data-estado]").innerHTML = pausado
                    ? '<span class="lm-pulse-dot is-off"></span>En pausa'
                    : '<span class="lm-pulse-dot"></span>En vivo · cada 5 s';
                if (!pausado) cargar();
                return;
            }
            const boton = evento.target.closest("[data-action]");
            if (!boton) return;
            const item = filas.find(s => String(s.sesion) === boton.closest("[data-row]")?.dataset.row);
            if (!item) return;
            const todas = boton.dataset.action === "cerrar-usuario";
            const confirmado = await ui.confirmar({
                titulo: todas ? "Desconectar usuario" : "Cerrar sesión",
                mensaje: todas
                    ? `Se cerrarán todas las conexiones abiertas de "${item.usuario}".`
                    : `Se cerrará la conexión de "${item.usuario}". Podrá volver a conectarse.`,
                textoConfirmar: todas ? "Desconectar" : "Cerrar sesión",
                peligro: true
            });
            if (!confirmado) return;
            try {
                await MonitorService.desconectar(todas ? { clienteId: item.clienteId, usuario: item.usuario } : { sesion: item.sesion });
                AppNotifications.success(todas ? "Usuario desconectado" : "Sesión cerrada");
                await cargar();
            } catch (error) {
                ui.notificarError(error);
            }
        });
        await cargar();
        timer = window.setInterval(() => {
            if (!pausado && !document.hidden) cargar();
        }, 5000);
    }

    function reset() {
        window.clearInterval(timer);
        timer = null;
        filas = [];
        pausado = false;
        caja = null;
        ui = null;
    }

    return { render, bind, reset };
}
