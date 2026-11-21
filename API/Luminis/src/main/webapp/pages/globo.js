(() => {
    const movimientoReducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const esClaro = () => document.documentElement.dataset.theme === "light";

    function vector(lat, lng) {
        const a = lat * Math.PI / 180;
        const b = lng * Math.PI / 180;
        return [Math.cos(a) * Math.sin(b), Math.sin(a), Math.cos(a) * Math.cos(b)];
    }

    function elegir(lista) {
        return lista[Math.floor(Math.random() * lista.length)];
    }

    function escapar(valor) {
        return String(valor ?? "").replace(/[&<>"']/g, caracter => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[caracter]);
    }

    function hora() {
        return new Date().toLocaleTimeString("es-DO", { hour12: false });
    }

    const CIUDADES = [
        ["Santo Domingo", "SDQ", 18.48, -69.93], ["Nueva York", "NYC", 40.71, -74.0], ["Miami", "MIA", 25.76, -80.19],
        ["Ciudad de México", "MEX", 19.43, -99.13], ["Bogotá", "BOG", 4.71, -74.07], ["Lima", "LIM", -12.05, -77.04],
        ["São Paulo", "GRU", -23.55, -46.63], ["Buenos Aires", "EZE", -34.6, -58.38], ["Santiago", "SCL", -33.45, -70.67],
        ["San Francisco", "SFO", 37.77, -122.42], ["Toronto", "YYZ", 43.65, -79.38], ["Madrid", "MAD", 40.42, -3.7],
        ["Londres", "LHR", 51.51, -0.13], ["París", "CDG", 48.86, 2.35], ["Berlín", "BER", 52.52, 13.4],
        ["Estocolmo", "ARN", 59.33, 18.07], ["Lagos", "LOS", 6.52, 3.38], ["Nairobi", "NBO", -1.29, 36.82],
        ["Johannesburgo", "JNB", -26.2, 28.05], ["El Cairo", "CAI", 30.04, 31.24], ["Dubái", "DXB", 25.2, 55.27],
        ["Bombay", "BOM", 19.08, 72.88], ["Singapur", "SIN", 1.35, 103.82], ["Tokio", "NRT", 35.68, 139.69],
        ["Seúl", "ICN", 37.57, 126.98], ["Hong Kong", "HKG", 22.32, 114.17], ["Sídney", "SYD", -33.87, 151.21],
        ["Yakarta", "CGK", -6.21, 106.85], ["Estambul", "IST", 41.01, 28.98], ["Panamá", "PTY", 8.98, -79.52]
    ].map(([nombre, codigo, lat, lng]) => ({ nombre, codigo, lat, lng, v: vector(lat, lng), pulso: Math.random() * 6 }));

    const TIPOS = [
        { tipo: "sensor.lectura", color: "#4ee6ff", datos: () => ({ temp: +(18 + Math.random() * 12).toFixed(1), hum: Math.round(40 + Math.random() * 40) }) },
        { tipo: "pedido.estado", color: "#ffc94a", datos: () => ({ pedido: `A-${Math.floor(100 + Math.random() * 900)}`, estado: elegir(["preparando", "en_camino", "entregado"]) }) },
        { tipo: "gps.posicion", color: "#7aa5ff", datos: () => ({ lat: +(Math.random() * 60 - 20).toFixed(3), lng: +(Math.random() * 160 - 80).toFixed(3) }) },
        { tipo: "precio.actualizado", color: "#ffc94a", datos: () => ({ par: elegir(["USD/DOP", "BTC", "EUR/USD"]), valor: +(Math.random() * 100).toFixed(2) }) },
        { tipo: "alerta.nueva", color: "#ff7a90", datos: () => ({ nivel: elegir(["baja", "media", "alta"]), origen: "monitor" }) },
        { tipo: "juego.movimiento", color: "#b28cff", datos: () => ({ x: Math.round(Math.random() * 400), y: Math.round(Math.random() * 300) }) },
        { tipo: "dashboard.kpi", color: "#3ee2a4", datos: () => ({ ventasHoy: Math.round(5000 + Math.random() * 40000) }) },
        { tipo: "chat.mensaje", color: "#7aa5ff", datos: () => ({ texto: elegir(["hola", "¿listo?", "en camino"]) }) }
    ];

    function crear({ contenedor, lienzo, tarjetas = [], terminal = null, ritmo = () => 1.5, mapa = "files/mapa/tierra.json", escala = 0.4 } = {}) {
        const contexto = lienzo?.getContext("2d");
        if (!contexto || !contenedor) return null;
        let puntos = new Float32Array(0);
        let ancho = 0;
        let alto = 0;
        let radio = 0;
        let giro = 1.25;
        let inclinacion = 0.32;
        let velocidad = 0.08;
        let arrastre = null;
        let visible = true;
        let ultimo = performance.now();
        let acumulado = 0;
        let activo = true;
        const observadores = [];
        let siguienteTarjeta = 0;
        const arcos = [];
        const ondas = [];

        fetch(mapa, { cache: "force-cache" })
            .then(respuesta => respuesta.json())
            .then(lista => {
                const salida = [];
                for (let i = 0; i < lista.length; i += 2) {
                    if (lista[i] < -60) continue;
                    salida.push(...vector(lista[i], lista[i + 1]));
                }
                puntos = new Float32Array(salida);
            })
            .catch(() => {
                puntos = new Float32Array(0);
            });

        const ajustar = () => {
            const escala = Math.min(window.devicePixelRatio || 1, 2);
            ancho = contenedor.clientWidth;
            alto = contenedor.clientHeight;
            lienzo.width = Math.round(ancho * escala);
            lienzo.height = Math.round(alto * escala);
            contexto.setTransform(escala, 0, 0, escala, 0, 0);
            radio = Math.min(ancho, alto) * escala;
        };

        const rotar = (x, y, z) => {
            const cg = Math.cos(giro);
            const sg = Math.sin(giro);
            const ci = Math.cos(inclinacion);
            const si = Math.sin(inclinacion);
            const x1 = x * cg - z * sg;
            const z1 = x * sg + z * cg;
            return [x1, y * ci - z1 * si, y * si + z1 * ci];
        };

        const pantalla = p => [ancho / 2 + p[0] * radio, alto / 2 - p[1] * radio];

        const oculto = p => p[2] < 0 && p[0] * p[0] + p[1] * p[1] < 1;

        const interpolar = (a, b, angulo, t) => {
            if (angulo < 1e-4) return a.slice();
            const s = Math.sin(angulo);
            const f1 = Math.sin((1 - t) * angulo) / s;
            const f2 = Math.sin(t * angulo) / s;
            const altura = 1 + Math.sin(Math.PI * t) * (0.06 + angulo * 0.16);
            return [(a[0] * f1 + b[0] * f2) * altura, (a[1] * f1 + b[1] * f2) * altura, (a[2] * f1 + b[2] * f2) * altura];
        };

        const lanzar = () => {
            const origen = elegir(CIUDADES);
            let destino = elegir(CIUDADES);
            while (destino === origen) destino = elegir(CIUDADES);
            const angulo = Math.acos(Math.max(-1, Math.min(1, origen.v[0] * destino.v[0] + origen.v[1] * destino.v[1] + origen.v[2] * destino.v[2])));
            const tipo = elegir(TIPOS);
            arcos.push({ origen, destino, angulo, tipo, datos: tipo.datos(), inicio: performance.now(), duracion: 1400 + angulo * 900 });
            origen.pulso = 0;
        };

        const anunciar = arco => {
            const latencia = Math.round(8 + arco.angulo * 22 + Math.random() * 12);
            const carga = JSON.stringify(arco.datos);
            const tarjeta = tarjetas.length ? tarjetas[siguienteTarjeta % tarjetas.length] : null;
            siguienteTarjeta += 1;
            if (tarjeta) {
                tarjeta.style.setProperty("--chip", arco.tipo.color);
                tarjeta.innerHTML = `<span class="lm-event-chip">${escapar(arco.tipo.tipo)}</span><strong>${escapar(arco.origen.nombre)} <em>→</em> ${escapar(arco.destino.nombre)}</strong><code>${escapar(carga)}</code><small>delivered · ${latencia} ms</small>`;
                tarjeta.classList.remove("is-fresh");
                void tarjeta.offsetWidth;
                tarjeta.classList.add("is-fresh");
            }
            if (terminal) {
                const linea = document.createElement("div");
                linea.className = "lm-terminal-line";
                linea.innerHTML = `<time>${hora()}</time><span class="dir">→</span><code><span class="t">${escapar(arco.tipo.tipo)}</span> ${escapar(arco.origen.codigo)}→${escapar(arco.destino.codigo)} <span class="v">${escapar(carga)}</span> ✓ ${latencia}ms</code>`;
                terminal.append(linea);
                while (terminal.children.length > 16) terminal.firstElementChild.remove();
            }
        };

        const dibujarEsfera = () => {
            const cx = ancho / 2;
            const cy = alto / 2;
            const claro = esClaro();
            const halo = contexto.createRadialGradient(cx, cy, radio * 0.92, cx, cy, Math.min(radio * 1.32, Math.min(ancho, alto) / 2));
            halo.addColorStop(0, claro ? "rgba(36,87,230,.28)" : "rgba(91,140,255,.5)");
            halo.addColorStop(0.35, claro ? "rgba(36,87,230,.1)" : "rgba(78,230,255,.16)");
            halo.addColorStop(1, "rgba(0,0,0,0)");
            contexto.fillStyle = halo;
            contexto.fillRect(0, 0, ancho, alto);
            const cuerpo = contexto.createRadialGradient(cx - radio * 0.35, cy - radio * 0.4, radio * 0.1, cx, cy, radio);
            cuerpo.addColorStop(0, claro ? "#ffffff" : "#1a2e6e");
            cuerpo.addColorStop(0.6, claro ? "#dfe8ff" : "#0b1640");
            cuerpo.addColorStop(1, claro ? "#c4d4ff" : "#050b26");
            contexto.beginPath();
            contexto.arc(cx, cy, radio, 0, Math.PI * 2);
            contexto.fillStyle = cuerpo;
            contexto.fill();
            contexto.lineWidth = 1.2;
            contexto.strokeStyle = claro ? "rgba(36,87,230,.35)" : "rgba(122,165,255,.45)";
            contexto.stroke();
        };

        const dibujarRejilla = () => {
            contexto.lineWidth = 0.6;
            contexto.strokeStyle = esClaro() ? "rgba(36,87,230,.09)" : "rgba(122,165,255,.08)";
            for (let lat = -60; lat <= 60; lat += 30) {
                contexto.beginPath();
                let dibujando = false;
                for (let lng = -180; lng <= 180; lng += 6) {
                    const p = rotar(...vector(lat, lng));
                    const [x, y] = pantalla(p);
                    if (p[2] < 0) {
                        dibujando = false;
                        continue;
                    }
                    if (dibujando) contexto.lineTo(x, y);
                    else contexto.moveTo(x, y);
                    dibujando = true;
                }
                contexto.stroke();
            }
            for (let lng = -180; lng < 180; lng += 30) {
                contexto.beginPath();
                let dibujando = false;
                for (let lat = -84; lat <= 84; lat += 6) {
                    const p = rotar(...vector(lat, lng));
                    const [x, y] = pantalla(p);
                    if (p[2] < 0) {
                        dibujando = false;
                        continue;
                    }
                    if (dibujando) contexto.lineTo(x, y);
                    else contexto.moveTo(x, y);
                    dibujando = true;
                }
                contexto.stroke();
            }
        };

        const dibujarTierra = () => {
            const claro = esClaro();
            const capas = claro
                ? ["rgba(36,87,230,.25)", "rgba(36,87,230,.45)", "rgba(36,87,230,.65)", "rgba(30,70,200,.9)"]
                : ["rgba(122,165,255,.25)", "rgba(122,165,255,.45)", "rgba(150,190,255,.7)", "rgba(200,220,255,.95)"];
            const grupos = [[], [], [], []];
            const cg = Math.cos(giro);
            const sg = Math.sin(giro);
            const ci = Math.cos(inclinacion);
            const si = Math.sin(inclinacion);
            for (let i = 0; i < puntos.length; i += 3) {
                const x = puntos[i];
                const y = puntos[i + 1];
                const z = puntos[i + 2];
                const x1 = x * cg - z * sg;
                const z1 = x * sg + z * cg;
                const z2 = y * si + z1 * ci;
                if (z2 < 0.02) continue;
                const y2 = y * ci - z1 * si;
                const nivel = z2 > 0.75 ? 3 : z2 > 0.45 ? 2 : z2 > 0.2 ? 1 : 0;
                grupos[nivel].push(ancho / 2 + x1 * radio, alto / 2 - y2 * radio);
            }
            const tamano = Math.max(1.3, radio / 170);
            grupos.forEach((grupo, nivel) => {
                contexto.fillStyle = capas[nivel];
                const lado = tamano * (0.75 + nivel * 0.12);
                for (let i = 0; i < grupo.length; i += 2) contexto.fillRect(grupo[i] - lado / 2, grupo[i + 1] - lado / 2, lado, lado);
            });
        };

        const dibujarCiudades = delta => {
            CIUDADES.forEach(ciudad => {
                ciudad.pulso += delta / 1000;
                const p = rotar(...ciudad.v);
                if (p[2] < 0.05) return;
                const [x, y] = pantalla(p);
                const fase = Math.min(1, ciudad.pulso / 1.6);
                if (fase < 1) {
                    contexto.beginPath();
                    contexto.arc(x, y, 3 + fase * 16, 0, Math.PI * 2);
                    contexto.strokeStyle = `rgba(255,201,74,${(1 - fase) * 0.8})`;
                    contexto.lineWidth = 1.4;
                    contexto.stroke();
                }
                contexto.beginPath();
                contexto.arc(x, y, 2.6, 0, Math.PI * 2);
                contexto.fillStyle = "#ffd877";
                contexto.shadowColor = "#ffc94a";
                contexto.shadowBlur = 10 * p[2];
                contexto.fill();
                contexto.shadowBlur = 0;
            });
        };

        const dibujarArcos = ahora => {
            contexto.lineCap = "round";
            for (let i = arcos.length - 1; i >= 0; i--) {
                const arco = arcos[i];
                const avance = (ahora - arco.inicio) / arco.duracion;
                if (avance >= 1) {
                    arcos.splice(i, 1);
                    ondas.push({ ciudad: arco.destino, inicio: ahora, color: arco.tipo.color });
                    arco.destino.pulso = 0;
                    anunciar(arco);
                    continue;
                }
                const cabeza = 1 - Math.pow(1 - avance, 2);
                const cola = Math.max(0, cabeza - 0.4);
                const muestras = 26;
                let previo = null;
                for (let m = 0; m <= muestras; m++) {
                    const t = cola + (cabeza - cola) * (m / muestras);
                    const p = rotar(...interpolar(arco.origen.v, arco.destino.v, arco.angulo, t));
                    const punto = pantalla(p);
                    if (previo && !oculto(p) && !previo.oculto) {
                        contexto.beginPath();
                        contexto.moveTo(previo.x, previo.y);
                        contexto.lineTo(punto[0], punto[1]);
                        contexto.strokeStyle = arco.tipo.color;
                        contexto.globalAlpha = Math.pow(m / muestras, 1.6) * 0.95;
                        contexto.lineWidth = 0.8 + (m / muestras) * 2;
                        contexto.stroke();
                    }
                    previo = { x: punto[0], y: punto[1], oculto: oculto(p) };
                }
                contexto.globalAlpha = 1;
                if (previo && !previo.oculto) {
                    contexto.beginPath();
                    contexto.arc(previo.x, previo.y, 3.2, 0, Math.PI * 2);
                    contexto.fillStyle = "#ffffff";
                    contexto.shadowColor = arco.tipo.color;
                    contexto.shadowBlur = 18;
                    contexto.fill();
                    contexto.shadowBlur = 0;
                }
            }
            for (let i = ondas.length - 1; i >= 0; i--) {
                const onda = ondas[i];
                const fase = (ahora - onda.inicio) / 900;
                if (fase >= 1) {
                    ondas.splice(i, 1);
                    continue;
                }
                const p = rotar(...onda.ciudad.v);
                if (p[2] < 0.05) continue;
                const [x, y] = pantalla(p);
                contexto.beginPath();
                contexto.arc(x, y, 4 + fase * 26, 0, Math.PI * 2);
                contexto.strokeStyle = onda.color;
                contexto.globalAlpha = 1 - fase;
                contexto.lineWidth = 2;
                contexto.stroke();
                contexto.globalAlpha = 1;
            }
        };

        const cuadro = ahora => {
            const delta = Math.min(64, ahora - ultimo);
            ultimo = ahora;
            if (visible && ancho > 0) {
                if (!arrastre) {
                    giro += (velocidad * delta) / 1000;
                    velocidad += (0.08 - velocidad) * 0.02;
                }
                acumulado += (delta / 1000) * Math.max(0, ritmo());
                while (acumulado >= 1 && arcos.length < 40) {
                    lanzar();
                    acumulado -= 1;
                }
                contexto.clearRect(0, 0, ancho, alto);
                dibujarEsfera();
                dibujarRejilla();
                dibujarTierra();
                dibujarArcos(ahora);
                dibujarCiudades(delta);
            }
            if (!movimientoReducido && activo) requestAnimationFrame(cuadro);
        };

        lienzo.addEventListener("pointerdown", evento => {
            arrastre = { x: evento.clientX, y: evento.clientY, giro, inclinacion, momento: performance.now() };
            lienzo.setPointerCapture(evento.pointerId);
            contenedor.classList.add("is-dragging");
        });
        lienzo.addEventListener("pointermove", evento => {
            if (!arrastre) return;
            const dx = evento.clientX - arrastre.x;
            const dy = evento.clientY - arrastre.y;
            const nuevo = arrastre.giro + dx * 0.006;
            const ahora = performance.now();
            velocidad = ((nuevo - giro) / Math.max(1, ahora - arrastre.momento)) * 1000;
            arrastre.momento = ahora;
            giro = nuevo;
            inclinacion = Math.max(-0.9, Math.min(0.9, arrastre.inclinacion + dy * 0.004));
        });
        const soltar = () => {
            arrastre = null;
            velocidad = Math.max(-3, Math.min(3, velocidad));
            contenedor.classList.remove("is-dragging");
        };
        lienzo.addEventListener("pointerup", soltar);
        lienzo.addEventListener("pointercancel", soltar);

        if ("ResizeObserver" in window) {
            const medidor = new ResizeObserver(ajustar);
            medidor.observe(contenedor);
            observadores.push(medidor);
        } else {
            window.addEventListener("resize", ajustar, { passive: true });
        }
        if ("IntersectionObserver" in window) {
            const vigia = new IntersectionObserver(entradas => {
                visible = entradas[0].isIntersecting;
            });
            vigia.observe(contenedor);
            observadores.push(vigia);
        }
        ajustar();
        if (movimientoReducido) {
            const estatico = () => {
                contexto.clearRect(0, 0, ancho, alto);
                dibujarEsfera();
                dibujarRejilla();
                dibujarTierra();
                dibujarCiudades(0);
            };
            window.setTimeout(estatico, 400);
            window.setTimeout(estatico, 1500);
        } else {
            requestAnimationFrame(cuadro);
        }
        const destruir = () => {
            activo = false;
            observadores.forEach(observador => observador.disconnect());
            window.removeEventListener("resize", ajustar);
        };
        return { lanzar, destruir };
    }

    window.LuminisGlobo = { crear };
})();
