const terminal = document.getElementById("terminal")

let step = 0
let buffer = ""
let currentInput = null

let origen = ""
let destino = ""

function scroll(){
terminal.scrollTop = terminal.scrollHeight
}

function print(text, cls=""){
const line = document.createElement("div")
line.className = "line " + cls
line.textContent = text
terminal.appendChild(line)
scroll()
}

function center(text,cls=""){

const line=document.createElement("div")
line.className="line center "+cls
line.textContent=text

terminal.appendChild(line)

scroll()

}

function welcome(){

const line = document.createElement("div")
line.className = "line welcome"
line.textContent = "BIENVENIDO"

terminal.appendChild(line)

}

function prompt(text){

const line = document.createElement("div")
line.className = "line"

const label = document.createElement("span")
label.className = "prompt"
label.textContent = "$ " + text

currentInput = document.createElement("span")

const cursor = document.createElement("span")
cursor.className = "cursor"

line.appendChild(label)
line.appendChild(currentInput)
line.appendChild(cursor)

terminal.appendChild(line)

scroll()

}

function updateInput(){

if(currentInput){
currentInput.textContent = buffer
}

}

document.addEventListener("keydown",(e)=>{

if(step===3) return

if(e.key==="Backspace"){
buffer = buffer.slice(0,-1)
updateInput()
return
}

if(e.key==="Enter"){

const value = buffer.trim()

buffer = ""

process(value)

return
}

if(e.key.length===1){

buffer+=e.key
updateInput()

}

})

function process(value){

if(step===1){

if(!/^[0-9]+$/.test(value)){

print("✖ El identificador debe contener solo números","error")

return

}

origen=value

prompt("Destino: ")

step=2

return

}

if(step===2){

if(!/^[0-9]+$/.test(value)){

print("✖ El destino debe contener solo números","error")

return

}

destino=value

if(destino===origen){

print("✖ El origen no puede ser igual al destino","error")

return

}

localStorage.setItem("origen",origen)
localStorage.setItem("destino",destino)

print("")
center("Teletransportando...","success")

step=3

setTimeout(()=>{

window.location.href="./chat/"

},2000)

}

}

function start(){

welcome()

setTimeout(()=>{

prompt("Tu identificador: ")

step=1

},600)

}

start()