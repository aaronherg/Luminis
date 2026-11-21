(() => {
    if (window.LuminisLoader) return;

    const instances = new WeakMap();
    const overlayMarkup = `
        <canvas aria-hidden="true"></canvas>
        <div class="fr-loading-visual" aria-hidden="true">
            <svg viewBox="0 0 420 310" focusable="false">
                <circle class="fr-loading-orbit" cx="210" cy="155" r="126"></circle>
                <circle class="fr-loading-orbit is-dashed" cx="210" cy="155" r="101"></circle>
                <g class="fr-loading-links"><path d="M210 155 112 88M210 155 308 88M210 155 331 176M210 155 266 232M210 155 154 232M210 155 89 176"></path></g>
                <g class="fr-loading-energy"><path d="M210 155 112 88M210 155 308 88M210 155 331 176M210 155 266 232M210 155 154 232M210 155 89 176"></path></g>
                <g class="fr-loading-energy delay-a"><path d="M210 155 112 88M210 155 308 88M210 155 331 176M210 155 266 232M210 155 154 232M210 155 89 176"></path></g>
                <g class="fr-loading-energy delay-b"><path d="M210 155 112 88M210 155 308 88M210 155 331 176M210 155 266 232M210 155 154 232M210 155 89 176"></path></g>
                <g>
                    <circle class="fr-loading-pulse" cx="112" cy="88" r="11"></circle><circle class="fr-loading-node" cx="112" cy="88" r="7"></circle><circle class="fr-loading-node-core" cx="112" cy="88" r="2.6"></circle>
                    <circle class="fr-loading-pulse delay" cx="308" cy="88" r="11"></circle><circle class="fr-loading-node" cx="308" cy="88" r="7"></circle><circle class="fr-loading-node-core" cx="308" cy="88" r="2.6"></circle>
                    <circle class="fr-loading-pulse" cx="331" cy="176" r="11"></circle><circle class="fr-loading-node" cx="331" cy="176" r="7"></circle><circle class="fr-loading-node-core" cx="331" cy="176" r="2.6"></circle>
                    <circle class="fr-loading-pulse delay" cx="266" cy="232" r="11"></circle><circle class="fr-loading-node" cx="266" cy="232" r="7"></circle><circle class="fr-loading-node-core" cx="266" cy="232" r="2.6"></circle>
                    <circle class="fr-loading-pulse" cx="154" cy="232" r="11"></circle><circle class="fr-loading-node" cx="154" cy="232" r="7"></circle><circle class="fr-loading-node-core" cx="154" cy="232" r="2.6"></circle>
                    <circle class="fr-loading-pulse delay" cx="89" cy="176" r="11"></circle><circle class="fr-loading-node" cx="89" cy="176" r="7"></circle><circle class="fr-loading-node-core" cx="89" cy="176" r="2.6"></circle>
                </g>
                <circle class="fr-loading-pulse" cx="210" cy="155" r="27"></circle><circle class="fr-loading-pulse delay" cx="210" cy="155" r="27"></circle>
                <circle class="fr-loading-orbit is-dashed" cx="210" cy="155" r="35"></circle><circle class="fr-loading-center" cx="210" cy="155" r="27"></circle>
                <circle class="fr-loading-center-core" cx="210" cy="155" r="20"></circle><path class="fr-loading-center-mark" d="M201 162h18M204 155h12M207 148h6"></path>
                <circle class="fr-loading-particle" cx="169" cy="75" r="1.8"></circle><circle class="fr-loading-particle" cx="258" cy="91" r="1.6"></circle>
                <circle class="fr-loading-particle" cx="350" cy="132" r="1.8"></circle><circle class="fr-loading-particle" cx="294" cy="211" r="1.6"></circle>
                <circle class="fr-loading-particle" cx="119" cy="214" r="1.6"></circle><circle class="fr-loading-particle" cx="73" cy="136" r="1.5"></circle>
            </svg>
        </div>`;

    function resolveTarget() {
        return (
            document.querySelector("[data-stage]") ||
            document.body
        );
    }

    function createInstance(target) {
        const overlay = document.createElement("div");
        const global = target === document.body;
        overlay.className = `fr-loading-screen ${global ? "is-global" : "is-local"}`;
        overlay.hidden = true;
        overlay.setAttribute("aria-hidden", "true");
        overlay.innerHTML = overlayMarkup;
        if (!global && getComputedStyle(target).position === "static") target.style.position = "relative";
        target.appendChild(overlay);

        const canvas = overlay.querySelector("canvas");
        const context = canvas.getContext("2d");
        const instance = {
            target,
            overlay,
            canvas,
            context,
            particles: [],
            activeRequests: 0,
            showTimer: 0,
            hideTimer: 0,
            animationFrame: 0,
            shownAt: 0,
            running: false
        };
        instances.set(target, instance);
        return instance;
    }

    function resize(instance) {
        const { canvas, context, target } = instance;
        const bounds = target === document.body ? { width: window.innerWidth, height: window.innerHeight } : target.getBoundingClientRect();
        const scale = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.max(1, Math.floor(bounds.width * scale));
        canvas.height = Math.max(1, Math.floor(bounds.height * scale));
        context.setTransform(scale, 0, 0, scale, 0, 0);
        instance.particles = Array.from({ length: 32 }, () => ({
            x: Math.random() * bounds.width,
            y: Math.random() * bounds.height,
            radius: 0.5 + Math.random() * 1.1,
            speed: 0.08 + Math.random() * 0.2,
            opacity: 0.08 + Math.random() * 0.18
        }));
    }

    function draw(instance) {
        if (!instance.running) return;
        const { context, target } = instance;
        const bounds = target === document.body ? { width: window.innerWidth, height: window.innerHeight } : target.getBoundingClientRect();
        context.clearRect(0, 0, bounds.width, bounds.height);
        for (const particle of instance.particles) {
            particle.y -= particle.speed;
            if (particle.y < -2) {
                particle.y = bounds.height + 2;
                particle.x = Math.random() * bounds.width;
            }
            context.beginPath();
            context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
            context.fillStyle = `rgba(18,107,43,${particle.opacity})`;
            context.fill();
        }
        instance.animationFrame = window.requestAnimationFrame(() => draw(instance));
    }

    function reveal(instance) {
        if (!instance.activeRequests || !instance.overlay.hidden) return;
        instance.overlay.hidden = false;
        instance.overlay.setAttribute("aria-hidden", "false");
        window.requestAnimationFrame(() => instance.overlay.classList.add("is-visible"));
        instance.shownAt = performance.now();
        instance.running = true;
        resize(instance);
        draw(instance);
    }

    function show(target = resolveTarget()) {
        if (!target || !target.isConnected) target = document.body;
        const instance = instances.get(target) || createInstance(target);
        instance.activeRequests += 1;
        window.clearTimeout(instance.hideTimer);
        if (instance.overlay.hidden && !instance.showTimer) {
            instance.showTimer = window.setTimeout(() => {
                instance.showTimer = 0;
                reveal(instance);
            }, 140);
        }
        return instance;
    }

    function hide(instance) {
        if (!instance) return;
        instance.activeRequests = Math.max(0, instance.activeRequests - 1);
        if (instance.activeRequests) return;
        window.clearTimeout(instance.showTimer);
        instance.showTimer = 0;
        const elapsed = performance.now() - instance.shownAt;
        instance.hideTimer = window.setTimeout(
            () => {
                if (instance.activeRequests) return;
                instance.overlay.classList.remove("is-visible");
                instance.overlay.setAttribute("aria-hidden", "true");
                window.setTimeout(() => {
                    if (!instance.activeRequests && instance.overlay.isConnected) instance.overlay.hidden = true;
                }, 240);
                instance.running = false;
                window.cancelAnimationFrame(instance.animationFrame);
            },
            Math.max(0, 280 - elapsed)
        );
    }

    window.addEventListener("resize", () => {
        document.querySelectorAll(".fr-loading-screen.is-visible").forEach(overlay => {
            const target = overlay.parentElement;
            const instance = instances.get(target);
            if (instance?.running) resize(instance);
        });
    });
    window.LuminisLoader = Object.freeze({ show, hide, resolveTarget });
})();
