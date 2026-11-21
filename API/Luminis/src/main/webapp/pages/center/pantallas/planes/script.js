let ui;
let root;
let plans = [];
let currencies = [];
const modes = ["Mensual","Bimestral","Trimestral","Semestral","Anual"].map(value => ({ valor: value, texto: value }));
const nil = value => value == null ? "Ilimitado" : ui.formato.numero(value);
export function render() {
 return `<div class="fr-toolbar"><p class="fr-toolbar-text">Configura peticiones, conexiones y retención para cada plan.</p><span class="fr-toolbar-spacer"></span><button class="fr-btn fr-btn-primary" data-new hidden>＋ Nuevo plan</button></div><section class="fr-panel"><div data-table></div></section>`;
}
async function load() {
 try {
  [plans,currencies] = await Promise.all([PlanService.listar(),MonedaService.listar()]);
  const create = ui.puede("planes","crear"), edit = ui.puede("planes","modificar"), remove = ui.puede("planes","cambiar_estado");
  root.querySelector("[data-new]").hidden = !create;
  root.querySelector("[data-table]").innerHTML = ui.tabla({ filas: plans || [], clave:"planId", vacio:"No hay planes registrados.", columnas:[
   {titulo:"Plan",valor:p=>`<span class="fr-cell-main">${ui.escapar(p.nombre)}</span><span class="fr-cell-sub">${ui.escapar(p.modalidad)}</span>`},
   {titulo:"Peticiones/mes",valor:p=>nil(p.peticionesMes)},
   {titulo:"Conexiones",valor:p=>nil(p.conexionesSimultaneas)},
   {titulo:"KB",valor:p=>p.tamanoMaximoMensajeKb == null ? "Sin límite" : ui.formato.numero(p.tamanoMaximoMensajeKb)},
   {titulo:"Retención",valor:p=>`${ui.formato.numero(p.retencionSegundos)} s`},
   {titulo:"Precio",valor:p=>ui.formato.moneda(p.precio,p.monedaSimbolo)},
   {titulo:"Por 1,000 extra",valor:p=>p.precioMilAdicional ? ui.formato.moneda(p.precioMilAdicional,p.monedaSimbolo) : "—"},
   {titulo:"Portal",valor:p=>p.publico ? "Visible" : "Oculto"},{titulo:"Estado",valor:p=>ui.estado(p.estado)},
   {titulo:"Acciones",clase:"is-actions",valor:()=> (edit?ui.botonAccion("edit","pencil","Editar"):"")+(remove?ui.botonAccion("delete","trash","Eliminar","is-danger"):"")}
  ]});
 } catch(error) { ui.notificarError(error); }
}
function form(plan) {
 const fresh = !plan;
 const currencyOptions = currencies.map(c=>({valor:c.monedaId,texto:`${c.codigo} · ${c.nombre}`}));
 ui.formulario({
  titulo:fresh?"Nuevo plan":"Editar plan",subtitulo:plan?.nombre,ancho:"lg",
  campos:`
   ${ui.campo({nombre:"nombre",etiqueta:"Nombre",valor:plan?.nombre,requerido:true,max:80})}
   ${ui.campo({nombre:"modalidad",etiqueta:"Modalidad",tipo:"select",valor:plan?.modalidad||"Mensual",opciones:modes})}
   ${ui.campo({nombre:"descripcion",etiqueta:"Descripción",tipo:"textarea",valor:plan?.descripcion,max:300,clase:"is-full"})}
   ${ui.campo({nombre:"monedaId",etiqueta:"Moneda",tipo:"select",valor:plan?.monedaId,opciones:currencyOptions})}
   <div class="fr-field">${ui.campo({nombre:"peticionesMes",etiqueta:"Peticiones al mes",tipo:"number",valor:plan?.peticionesMes??"",atributos:'min="1" step="1"'})}${ui.campo({nombre:"peticionesIlimitadas",etiqueta:"Ilimitadas",tipo:"checkbox",valor:!fresh&&plan.peticionesMes==null})}</div>
   <div class="fr-field">${ui.campo({nombre:"conexionesSimultaneas",etiqueta:"Conexiones simultáneas",tipo:"number",valor:plan?.conexionesSimultaneas??"",atributos:'min="1" step="1"'})}${ui.campo({nombre:"conexionesIlimitadas",etiqueta:"Ilimitadas",tipo:"checkbox",valor:!fresh&&plan.conexionesSimultaneas==null})}</div>
   <div class="fr-field">${ui.campo({nombre:"tamanoMaximoMensajeKb",etiqueta:"Tamaño máx. de mensaje (KB)",tipo:"number",valor:plan?.tamanoMaximoMensajeKb??"",atributos:'min="1" step="1"'})}${ui.campo({nombre:"mensajeIlimitado",etiqueta:"Sin límite",tipo:"checkbox",valor:!fresh&&plan.tamanoMaximoMensajeKb==null})}</div>
   ${ui.campo({nombre:"retencionSegundos",etiqueta:"Retención de cola offline (s)",tipo:"number",valor:plan?.retencionSegundos??10,requerido:true,atributos:'min="0" max="3600" step="1"'})}
   ${ui.campo({nombre:"precio",etiqueta:"Precio",tipo:"number",valor:plan?.precio??"",requerido:true,atributos:'min="0" step="0.01"'})}
   ${ui.campo({nombre:"precioMilAdicional",etiqueta:"Precio por 1,000 peticiones adicionales",tipo:"number",valor:plan?.precioMilAdicional??0,atributos:'min="0" step="0.0001"'})}
   ${ui.campo({nombre:"orden",etiqueta:"Orden",tipo:"number",valor:plan?.orden??0,atributos:'min="0" step="1"'})}
   ${ui.campo({nombre:"caracteristicas",etiqueta:"Características",tipo:"textarea",valor:(plan?.caracteristicas||"").split("|").join("\n"),clase:"is-full",ayuda:"Una característica por línea."})}
   <div class="fr-field is-full fr-checks-inline">${ui.campo({nombre:"publico",etiqueta:"Visible en el portal",tipo:"checkbox",valor:fresh?true:plan.publico})}${ui.campo({nombre:"destacado",etiqueta:"Destacado",tipo:"checkbox",valor:plan?.destacado})}</div>
   ${fresh?"":ui.campo({nombre:"estado",etiqueta:"Estado",tipo:"select",valor:plan.estado,opciones:[{valor:"Activo",texto:"Activo"},{valor:"Inactivo",texto:"Inactivo"}]})}`,
  alAbrir(formElement) {
   const link = (check,input,disableExtra) => {
    const apply = () => { const on=formElement.elements[check].checked; formElement.elements[input].disabled=on; if(on) formElement.elements[input].value=""; if(disableExtra){formElement.elements[disableExtra].disabled=on;if(on)formElement.elements[disableExtra].value="0";} };
    formElement.elements[check].addEventListener("change",apply); apply();
   };
   link("peticionesIlimitadas","peticionesMes","precioMilAdicional");link("conexionesIlimitadas","conexionesSimultaneas");link("mensajeIlimitado","tamanoMaximoMensajeKb");
  },
  async alGuardar(formElement) {
   const unlimited = ["peticionesIlimitadas","conexionesIlimitadas","mensajeIlimitado"].map(name=>formElement.elements[name].checked);
   ui.exigir(formElement,[["nombre","El nombre es obligatorio."],["monedaId","Selecciona una moneda."],["precio","El precio debe ser mayor o igual a cero.",v=>v!==""&&Number(v)>=0],["peticionesMes","Las peticiones deben ser un entero positivo.",v=>unlimited[0]||(/^\d+$/.test(v)&&Number(v)>0)],["conexionesSimultaneas","Las conexiones deben ser un entero positivo.",v=>unlimited[1]||(/^\d+$/.test(v)&&Number(v)>0)],["tamanoMaximoMensajeKb","El tamaño debe ser un entero positivo.",v=>unlimited[2]||(/^\d+$/.test(v)&&Number(v)>0)],["retencionSegundos","La retención debe estar entre 0 y 3600 segundos.",v=>/^\d+$/.test(v)&&Number(v)<=3600],["precioMilAdicional","El adicional no puede ser negativo.",v=>!v||Number(v)>=0]]);
   const data=ui.valores(formElement);
   data.monedaId=Number(data.monedaId);data.peticionesMes=unlimited[0]?null:Number(data.peticionesMes);data.conexionesSimultaneas=unlimited[1]?null:Number(data.conexionesSimultaneas);data.tamanoMaximoMensajeKb=unlimited[2]?null:Number(data.tamanoMaximoMensajeKb);data.retencionSegundos=Number(data.retencionSegundos);data.precioMilAdicional=unlimited[0]?0:Number(data.precioMilAdicional||0);
   ["peticionesIlimitadas","conexionesIlimitadas","mensajeIlimitado"].forEach(name=>delete data[name]);
   data.caracteristicas=String(data.caracteristicas||"").split(/\r?\n/).map(x=>x.trim().replace(/\|/g,"/")).filter(Boolean).join("|");
   if(fresh) await PlanService.agregar(data); else await PlanService.actualizar({...data,planUuid:plan.planUuid});
   AppNotifications.success(fresh?"Plan creado correctamente":"Plan actualizado correctamente");await load();
  }
 });
}
export async function bind(stage,api){ui=api;root=stage;root.querySelector("[data-new]").addEventListener("click",()=>form(null));root.querySelector("[data-table]").addEventListener("click",async event=>{const b=event.target.closest("[data-action]");if(!b)return;const p=plans.find(x=>String(x.planId)===b.closest("[data-row]")?.dataset.row);if(!p)return;if(b.dataset.action==="edit")form(p);if(b.dataset.action==="delete"){if(await ui.confirmar({titulo:"Eliminar plan",mensaje:`¿Eliminar ${p.nombre}?`,peligro:true})){try{await PlanService.eliminar(p.planId);await load()}catch(error){ui.notificarError(error)}}}});await load()}
export function reset(){plans=[]}