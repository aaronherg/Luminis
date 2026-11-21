(() => {
    const base = new URL("./", document.currentScript?.src || location.href);
    const storageKey = "luminis.language";
    const codes = ["es", "en"];
    const textSources = new WeakMap();
    const renderedTexts = new WeakMap();
    const attributeSources = new WeakMap();
    const renderedAttributes = new WeakMap();
    const catalogs = new Map();
    const lookupCaches = new WeakMap();
    let current = "es";
    let observer;
    let phraseCatalogPromise;
    let languageRequest = 0;

    async function load(code) {
        if (catalogs.has(code)) return catalogs.get(code);
        const response = await fetch(new URL(`${code}.json`, base), { cache: "no-cache" });
        if (!response.ok) throw new Error(`No se pudo cargar el idioma ${code}.`);
        const catalog = await response.json();
        phraseCatalogPromise ||= Promise.all(
            ["public.json", "ui.json", "center.json", "customer.json"].map(
                file =>
                    fetch(new URL(file, base), { cache: "no-cache" })
                        .then(result => (result.ok ? result.json() : {}))
                        .catch(() => ({}))
            )
        );
        const phraseCatalogs = await phraseCatalogPromise;
        const phraseMessages = Object.assign({}, ...phraseCatalogs.map(item => item.messages || {}));
        const supplements = Object.fromEntries(
            Object.entries(phraseMessages).map(([source, translations]) => [source, translations?.[code] ?? source])
        );
        catalog.messages = { ...(catalog.messages || {}), ...supplements };
        catalogs.set(code, catalog);
        return catalog;
    }

    function sourceText(node) {
        if (!textSources.has(node)) textSources.set(node, node.nodeValue);
        return textSources.get(node);
    }

    function applyText(node, messages) {
        const parent = node.parentElement;
        if (parent?.closest("script,style,noscript,[data-i18n-skip],[data-i18n-data]")) return;
        const cell = parent?.closest("td");
        if (cell && !parent.closest("[data-i18n]")) return;
        let source = sourceText(node);
        const lastRendered = renderedTexts.get(node);
        if (lastRendered !== undefined && node.nodeValue !== lastRendered) {
            source = node.nodeValue;
            textSources.set(node, source);
        }
        const clean = source.trim();
        if (!clean) return;
        const translated = lookup(clean, messages);
        if (translated === undefined) {
            renderedTexts.set(node, source);
            return;
        }
        const start = source.length - source.trimStart().length;
        const end = source.length - source.trimEnd().length;
        const next = `${source.slice(0, start)}${translated}${end ? source.slice(source.length - end) : ""}`;
        renderedTexts.set(node, next);
        if (node.nodeValue !== next) node.nodeValue = next;
    }

    function lookup(source, messages) {
        let cache = lookupCaches.get(messages);
        if (!cache) {
            cache = new Map();
            lookupCaches.set(messages, cache);
        }
        if (cache.has(source)) return cache.get(source);
        const translated = lookupUncached(source, messages);
        cache.set(source, translated);
        return translated;
    }

    function lookupUncached(source, messages) {
        if (messages[source] !== undefined) return messages[source];
        for (const [pattern, translated] of Object.entries(messages)) {
            if (!pattern.includes("{")) continue;
            const names = [];
            const expressionSource = pattern
                .split(/(\{[a-zA-Z0-9_]+\})/g)
                .map(part => {
                    const placeholder = part.match(/^\{([a-zA-Z0-9_]+)\}$/);
                    if (placeholder) {
                        names.push(placeholder[1]);
                        return "(.+?)";
                    }
                    return part.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
                })
                .join("");
            const expression = new RegExp(`^${expressionSource}$`);
            const match = source.match(expression);
            if (!match) continue;
            return names.reduce((result, name, index) => result.split(`{${name}}`).join(match[index + 1] ?? ""), translated);
        }
        return undefined;
    }

    function applyAttributes(element, messages) {
        if (element.closest("[data-i18n-skip],[data-i18n-data]")) return;
        for (const name of ["title", "aria-label", "placeholder", "alt", "content", "value"]) {
            if (!element.hasAttribute(name)) continue;
            if (
                name === "value" &&
                !(element.matches("button") || element.matches('input[type="button"],input[type="submit"],input[type="reset"]'))
            )
                continue;
            let values = attributeSources.get(element);
            if (!values) {
                values = new Map();
                attributeSources.set(element, values);
            }
            if (!values.has(name)) values.set(name, element.getAttribute(name));
            const lastValues = renderedAttributes.get(element) || new Map();
            renderedAttributes.set(element, lastValues);
            if (lastValues.has(name) && element.getAttribute(name) !== lastValues.get(name)) values.set(name, element.getAttribute(name));
            const source = values.get(name);
            const translated = lookup(source?.trim(), messages);
            if (translated !== undefined) {
                lastValues.set(name, translated);
                if (element.getAttribute(name) !== translated) element.setAttribute(name, translated);
            }
        }
    }

    function translateTree(root = document.body) {
        const catalog = catalogs.get(current);
        if (!catalog || !root) return;
        const messages = catalog.messages || {};
        if (root.nodeType === Node.TEXT_NODE) {
            applyText(root, messages);
            return;
        }
        if (root.nodeType === Node.ELEMENT_NODE) applyAttributes(root, messages);
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        while (walker.nextNode()) {
            const node = walker.currentNode;
            applyText(node, messages);
        }
        if (root.nodeType === Node.ELEMENT_NODE) root.querySelectorAll("*").forEach(element => applyAttributes(element, messages));
    }

    async function setLanguage(code) {
        if (!codes.includes(code)) return false;
        const request = ++languageRequest;
        try {
            await load(code);
            if (request !== languageRequest) return true;
            if (code === current) {
                localStorage.setItem(storageKey, code);
                return true;
            }
            current = code;
            document.documentElement.lang = code;
            localStorage.setItem(storageKey, code);
            translateTree(document.head);
            translateTree();
            document.dispatchEvent(new CustomEvent("luminis:languagechange", { detail: { code, catalog: catalogs.get(code) } }));
            return true;
        } catch (error) {
            console.error("No se pudo cambiar el idioma de Luminis.", error);
            return false;
        }
    }

    async function init() {
        let saved = "es";
        try {
            saved = localStorage.getItem(storageKey) || "es";
        } catch (_) {
            saved = "es";
        }
        if (!codes.includes(saved)) saved = "es";
        await Promise.all(
            codes.map(code =>
                load(code).catch(error => {
                    console.error(error);
                    return null;
                })
            )
        );
        current = catalogs.has(saved) ? saved : "es";
        document.documentElement.lang = current;
        translateTree(document.head);
        translateTree();
        if (!observer) {
            observer = new MutationObserver(records => {
                for (const record of records) {
                    if (record.type === "characterData") applyText(record.target, catalogs.get(current)?.messages || {});
                    else
                        record.addedNodes.forEach(node => {
                            if (node.nodeType === Node.ELEMENT_NODE || node.nodeType === Node.TEXT_NODE) translateTree(node);
                        });
                    if (record.type === "attributes") applyAttributes(record.target, catalogs.get(current)?.messages || {});
                }
            });
            observer.observe(document.body, {
                subtree: true,
                childList: true,
                characterData: true,
                attributes: true,
                attributeFilter: ["title", "aria-label", "placeholder", "alt", "value", "content"]
            });
        }
        document.dispatchEvent(new CustomEvent("luminis:i18nready", { detail: { code: current } }));
        return current;
    }

    window.AppI18n = {
        codes,
        init,
        setLanguage,
        translate: translateTree,
        current: () => current,
        catalog: (code = current) => catalogs.get(code) || null
    };
    init();
})();
