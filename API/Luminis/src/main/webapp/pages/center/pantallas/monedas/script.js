let ui;
let raiz;
let monedas = [];

export function render() {
    return `
        <div class="fr-toolbar">
            <span class="fr-toolbar-spacer"></span>
            <button class="fr-btn fr-btn-primary" type="button" data-nuevo hidden>${LuminisPanel.icono("plus")}<span>Nueva moneda</span></button>
        </div>
        <section class="fr-panel"><div data-lista></div></section>`;
}

async function cargar() {
    try {
        monedas = (await MonedaService.listar()) || [];
        const modificar = ui.puede("monedas", "modificar");
        raiz.querySelector("[data-lista]").innerHTML = ui.tabla({
            filas: monedas,
            clave: "monedaId",
            vacio: "No hay monedas registradas.",
            columnas: [
                { titulo: "Código", valor: m => `<span class="fr-mono">${ui.escapar(m.codigo)}</span>` },
                { titulo: "Nombre", valor: m => `<span class="fr-cell-main">${ui.escapar(m.nombre)}</span>` },
                { titulo: "Símbolo", valor: m => ui.escapar(m.simbolo) },
                { titulo: "Tasa", clase: "is-right", valor: m => ui.formato.numero(m.tasa, 4) },
                { titulo: "Estado", valor: m => ui.estado(m.estado) },
                {
                    titulo: "Acciones",
                    clase: "is-actions",
                    valor: () => (modificar ? ui.botonAccion("editar", "pencil", "Editar") + ui.botonAccion("eliminar", "trash", "Eliminar", "is-danger") : "")
                }
            ]
        });
    } catch (error) {
        ui.notificarError(error);
    }
}

function formulario(moneda) {
    const nueva = !moneda;
    ui.formulario({
        titulo: nueva ? "Nueva moneda" : "Editar moneda",
        ancho: "sm",
        campos: `
            ${ui.campo({ nombre: "codigo", etiqueta: "Código ISO", valor: moneda?.codigo, requerido: true, max: 3, ayuda: "Tres letras, por ejemplo USD o DOP.", atributos: 'style="text-transform:uppercase"' })}
            ${ui.campo({ nombre: "simbolo", etiqueta: "Símbolo", valor: moneda?.simbolo, requerido: true, max: 5 })}
            ${ui.campo({ nombre: "nombre", etiqueta: "Nombre", valor: moneda?.nombre, requerido: true, max: 60, clase: "is-full" })}
            ${ui.campo({ nombre: "tasa", etiqueta: "Tasa", tipo: "number", valor: moneda?.tasa ?? 1, requerido: true, atributos: 'min="0.0001" step="0.0001"' })}
            ${nueva ? "" : ui.campo({ nombre: "estado", etiqueta: "Estado", tipo: "select", valor: moneda.estado, opciones: [{ valor: "Activo", texto: "Activo" }, { valor: "Inactivo", texto: "Inactivo" }] })}`,
        async alGuardar(form) {
            ui.exigir(form, [
                ["codigo", "El código debe tener 3 letras.", valor => /^[A-Za-z]{3}$/.test(valor)],
                ["simbolo", "El símbolo es obligatorio."],
                ["nombre", "El nombre es obligatorio."],
                ["tasa", "La tasa debe ser mayor que cero.", valor => Number(valor) > 0]
            ]);
            const datos = ui.valores(form);
            datos.codigo = datos.codigo.toUpperCase();
            if (nueva) await MonedaService.agregar(datos);
            else await MonedaService.actualizar({ ...datos, monedaUuid: moneda.monedaUuid });
            AppNotifications.success(nueva ? "Moneda creada correctamente" : "Moneda actualizada correctamente");
            await cargar();
        }
    });
}

async function eliminar(moneda) {
    const ok = await ui.confirmar({
        titulo: "Eliminar moneda",
        mensaje: `Se eliminará ${moneda.nombre}. No es posible si algún plan la utiliza.`,
        textoConfirmar: "Eliminar",
        peligro: true
    });
    if (!ok) return;
    try {
        await MonedaService.eliminar(moneda.monedaId);
        AppNotifications.success("Moneda eliminada correctamente");
        await cargar();
    } catch (error) {
        ui.notificarError(error);
    }
}

export async function bind(stage, api) {
    ui = api;
    raiz = stage;
    const nuevo = raiz.querySelector("[data-nuevo]");
    nuevo.hidden = !ui.puede("monedas", "crear");
    nuevo.addEventListener("click", () => formulario(null));
    raiz.querySelector("[data-lista]").addEventListener("click", event => {
        const boton = event.target.closest("[data-action]");
        if (!boton) return;
        const moneda = monedas.find(m => String(m.monedaId) === boton.closest("[data-row]")?.dataset.row);
        if (!moneda) return;
        if (boton.dataset.action === "editar") formulario(moneda);
        if (boton.dataset.action === "eliminar") eliminar(moneda);
    });
    await cargar();
}

export function reset() {
    monedas = [];
}

export function skeleton() {
    return window.LuminisSk.lista({ filtros: 1, columnas: 4 });
}
