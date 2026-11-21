(function configureLuminis() {
    const path = window.location.pathname || "/";
    const pagesIndex = path.indexOf("/pages/");
    const contextPath = pagesIndex >= 0 ? path.slice(0, pagesIndex) : path.replace(/\/[^/]*$/, "");
    window.LuminisConfig = Object.freeze({
        contextPath,
        apiBaseUrl: `${window.location.origin}${contextPath}`
    });
})();
