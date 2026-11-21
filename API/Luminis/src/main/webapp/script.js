(() => {
    const raiz = document.getElementById("app-root");
    const pantalla = raiz.querySelector(".boot-screen");
    const barra = raiz.querySelector("[data-boot-bar]");
    const estado = raiz.querySelector("[data-boot-status]");
    const lienzo = raiz.querySelector("[data-boot-canvas]");
    const movimientoReducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const duracion = movimientoReducido ? 700 : 2600;
    const inicio = performance.now();
    const ESTADOS = [
        [0, "Encendiendo la red"],
        [0.3, "Conectando nodos"],
        [0.6, "Sincronizando eventos"],
        [0.88, "Todo listo"]
    ];
    let indiceEstado = 0;
    let terminado = false;

    function cambiarEstado(texto) {
        estado.classList.add("is-changing");
        window.setTimeout(() => {
            estado.textContent = texto;
            estado.classList.remove("is-changing");
        }, 180);
    }

    function crearParticulas() {
        const contexto = lienzo?.getContext("2d");
        if (!contexto || movimientoReducido) return () => {};
        let ancho = 0;
        let alto = 0;
        const particulas = [];
        const claro = document.documentElement.dataset.theme === "light";
        const colores = claro ? ["217,143,0", "36,87,230", "10,168,201"] : ["255,201,74", "122,165,255", "78,230,255"];
        const ajustar = () => {
            const escala = Math.min(window.devicePixelRatio || 1, 2);
            ancho = window.innerWidth;
            alto = window.innerHeight;
            lienzo.width = ancho * escala;
            lienzo.height = alto * escala;
            contexto.setTransform(escala, 0, 0, escala, 0, 0);
        };
        const nueva = () => {
            const angulo = Math.random() * Math.PI * 2;
            const distancia = Math.hypot(ancho, alto) * (0.35 + Math.random() * 0.25);
            return {
                angulo,
                distancia,
                velocidad: 0.006 + Math.random() * 0.01,
                giro: (Math.random() - 0.5) * 0.012,
                tamano: 0.8 + Math.random() * 1.8,
                color: colores[Math.floor(Math.random() * colores.length)]
            };
        };
        ajustar();
        window.addEventListener("resize", ajustar, { passive: true });
        for (let i = 0; i < 140; i++) {
            const particula = nueva();
            particula.distancia *= Math.random();
            particulas.push(particula);
        }
        let activo = true;
        const cuadro = () => {
            if (!activo) return;
            const cx = ancho / 2;
            const cy = alto / 2 - 40;
            contexto.clearRect(0, 0, ancho, alto);
            contexto.globalCompositeOperation = claro ? "source-over" : "lighter";
            particulas.forEach((p, indice) => {
                const xAntes = cx + Math.cos(p.angulo) * p.distancia;
                const yAntes = cy + Math.sin(p.angulo) * p.distancia;
                p.distancia *= 1 - p.velocidad;
                p.distancia -= 0.6;
                p.angulo += p.giro;
                if (p.distancia < 46) {
                    particulas[indice] = nueva();
                    return;
                }
                const x = cx + Math.cos(p.angulo) * p.distancia;
                const y = cy + Math.sin(p.angulo) * p.distancia;
                const brillo = Math.min(1, 160 / p.distancia + 0.15);
                contexto.strokeStyle = `rgba(${p.color},${brillo * 0.55})`;
                contexto.lineWidth = p.tamano;
                contexto.beginPath();
                contexto.moveTo(xAntes, yAntes);
                contexto.lineTo(x, y);
                contexto.stroke();
                contexto.fillStyle = `rgba(${p.color},${brillo})`;
                contexto.beginPath();
                contexto.arc(x, y, p.tamano, 0, Math.PI * 2);
                contexto.fill();
            });
            contexto.globalCompositeOperation = "source-over";
            requestAnimationFrame(cuadro);
        };
        requestAnimationFrame(cuadro);
        return () => {
            activo = false;
        };
    }

    function salir(detener) {
        if (terminado) return;
        terminado = true;
        pantalla.classList.add("is-leaving");
        window.setTimeout(() => {
            detener();
            window.location.replace("pages/");
        }, movimientoReducido ? 150 : 650);
    }

    const detener = crearParticulas();
    const avanzar = ahora => {
        const avance = Math.min(1, (ahora - inicio) / duracion);
        const suave = 1 - Math.pow(1 - avance, 2.2);
        barra.style.width = `${(suave * 100).toFixed(1)}%`;
        while (indiceEstado < ESTADOS.length - 1 && suave >= ESTADOS[indiceEstado + 1][0]) {
            indiceEstado += 1;
            cambiarEstado(ESTADOS[indiceEstado][1]);
        }
        if (avance < 1) requestAnimationFrame(avanzar);
        else salir(detener);
    };
    requestAnimationFrame(avanzar);
})();
