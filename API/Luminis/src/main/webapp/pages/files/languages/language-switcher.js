(() => {
    const controls = [...document.querySelectorAll("[data-language-switcher]")];
    if (!controls.length) return;

    const sync = () => {
        const language = window.AppI18n?.current?.() || "es";
        controls.forEach(control => {
            control.value = language;
        });
    };

    controls.forEach(control => {
        control.addEventListener("change", async () => {
            if (!window.AppI18n) return;
            const changed = await window.AppI18n.setLanguage(control.value);
            if (!changed) sync();
        });
    });

    document.addEventListener("luminis:i18nready", sync);
    document.addEventListener("luminis:languagechange", sync);
    sync();
})();
