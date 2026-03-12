// =========================
// VARIABLES GLOBALES
// =========================
let socket = null
let tempMap = {}
let origenMap = {}

const messages = document.getElementById("messages")
const input = document.getElementById("mensaje")
const status = document.getElementById("status")

const origen = localStorage.getItem("origen")
const destino = localStorage.getItem("destino")

// =========================
// VALIDACION DE SESION
// =========================
function conexionRechazada(){
	const body = document.querySelector(".terminal-body")
	body.innerHTML = `<div class="terminal-reject">CONEXION RECHAZADA</div>`
	localStorage.removeItem("origen")
	localStorage.removeItem("destino")
	setTimeout(()=> window.location.href="../", 3000)
}

function validarSesion(){
	if(!origen || !destino) return false
	const o = origen.trim()
	const d = destino.trim()
	if(o==="" || d==="") return false
	if(!/^[0-9]+$/.test(o) || !/^[0-9]+$/.test(d)) return false
	if(o===d) return false
	return true
}

if(!validarSesion()){
	conexionRechazada()
	throw new Error("Sesion invalida")
}

// =========================
// UTILIDADES
// =========================
function scroll(){
	messages.scrollTop = messages.scrollHeight
}

function addMessage(text, type, state="", id="", showSeenButton=false, from=""){
	const div = document.createElement("div")
	div.className = "message "+type
	div.id = id

	if(from){
		origenMap[id] = from
	}

	let btn=""
	if(showSeenButton){
		btn=`<button class="btn-seen" onclick="marcarVisto('${id}')">✔ visto</button>`
	}

	div.innerHTML=`
		<div class="id">$ (${from})</div>
		<div class="msg">&gt; ${text}</div>
		<div class="meta">${state} ${btn}</div>
	`

	messages.appendChild(div)
	scroll()
}

function updateState(id,state){
	const el = document.getElementById(id)
	if(!el) return
	el.querySelector(".meta").innerHTML = state
}

function markDisappeared(id){
	const el = document.getElementById(id)
	if(el){
		el.querySelector(".meta").innerHTML = "❌ Desaparecido"
		el.style.opacity = "0.6"
	}
}

function markDesconocido(id){
	const el = document.getElementById(id)
	if(el){
		el.querySelector(".meta").innerHTML = "❌ Desconocido"
		el.style.opacity = "0.6"
	}
}

// =========================
// WEBSOCKET
// =========================
function connect(){

	if(socket && socket.readyState===1) return

	const protocol = location.protocol === "https:" ? "wss://" : "ws://"
	socket = new WebSocket(protocol + "localhost:2345")

	socket.onopen = function(){
		status.innerText = "Conectado"
		socket.send(JSON.stringify({origen: origen}))
	}

	socket.onclose = function(){
		status.innerText = "Desconectado"
	}

	socket.onerror = function(err){
		console.error("WebSocket error:", err)
	}

	socket.onmessage = function(event){

		const data = JSON.parse(event.data)

		switch(data.type){

			case "sent":

				const tempId = tempMap.last
				const elSent = document.getElementById(tempId)

				if(elSent){
					elSent.id = "msg-"+data.mensajeId
					updateState("msg-"+data.mensajeId,"✔ Enviado")
				}

			break

			case "delivered":
				updateState("msg-"+data.mensajeId,"✔ Entregado")
			break

			case "seen":
				updateState("msg-"+data.mensajeId,"✔✔ Visto")
			break

			case "message":

				addMessage(
					data.mensaje,
					"other",
					"",
					"msg-"+data.mensajeId,
					true,
					data.origen
				)

			break

			case "desaparecido":
				markDisappeared("msg-"+data.mensajeId)
			break

			case "desconocido":
				markDesconocido(tempMap.last)
			break

		}

	}

}

// =========================
// ENVIAR MENSAJE
// =========================
function sendMessage(){

	if(!socket || socket.readyState!==1){
		alert("Conectando al servidor, intenta de nuevo en unos segundos...")
		return
	}

	const mensaje = input.value.trim()
	if(!mensaje) return

	const tempId = "tmp-"+Date.now()
	tempMap.last = tempId

	socket.send(JSON.stringify({
		origen: origen,
		destino: destino,
		mensaje: mensaje
	}))

	addMessage(mensaje,"me","⏳ Enviando...", tempId,false,destino)

	input.value=""
}

// =========================
// MARCAR VISTO
// =========================
function marcarVisto(id){

	const mensajeId = id.replace("msg-","")

	const destinoMensaje = origenMap[id]

	if(socket && socket.readyState===1){

		socket.send(JSON.stringify({
			type:"seen",
			origen: origen,
			destino: destinoMensaje,
			mensajeId: mensajeId
		}))

	}

	const el=document.getElementById(id)
	if(el) el.querySelector(".meta").innerHTML="✔✔ Visto"

}

// =========================
// EVENTOS INPUT + BOTON
// =========================
input.addEventListener("keypress", function(e){
	if(e.key==="Enter"){
		e.preventDefault()
		sendMessage()
	}
})

document.getElementById("sendBtn").addEventListener("click",sendMessage)

// =========================
// INICIAR CONEXION
// =========================
connect()
input.focus()