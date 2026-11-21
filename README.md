<div align="center">

  <img src="https://github.com/aaronherg/aaronherg/blob/main/recursos/luminis.png?raw=true" alt="Luminis" width="30%"/>

  <p><strong>Luminis</strong></p>
  <p>Red WebSocket en tiempo real como servicio: envía eventos, telemetría, notificaciones o mensajes a cualquier dispositivo, con presencia, confirmación de entrega, cola offline y API REST.</p>

</div>

<hr>

<div align="center">
  <h2>Esquema</h2>
  <img src="https://github.com/aaronherg/aaronherg/blob/main/recursos/luminis_esquema.png?raw=true" width="50%" alt="Esquema de Luminis">
</div>

## Plataforma

Luminis mueve cualquier JSON o texto en tiempo real: lecturas de sensores, estados de pedidos, posiciones GPS, precios, alertas, jugadas o mensajes de chat. Los planes se miden por peticiones mensuales, conexiones simultáneas, tamaño máximo de evento y retención de la cola offline. Un límite nulo representa capacidad ilimitada; los planes pueden cobrar peticiones adicionales por cada mil.

- **Portal**: globo 3D con el tráfico de la red en vivo, casos de uso, guía para desarrolladores y planes públicos.
- **Center** (administración): dashboard con la red en vivo, clientes, suscripciones, pagos, monitor de conexiones, planes, monedas, usuarios, roles e informes.
- **Customer** (cliente): dashboard con consumo y red en vivo, monitor, consola WebSocket, suscripción, pagos, API keys, usuarios, roles y perfil.

## Documentación para desarrolladores

La guía completa (conexión, envío y recepción de eventos, ciclo de entrega, presencia, errores, códigos de cierre, API REST, consumo y ejemplos en JavaScript, Node.js y Python) está publicada en el propio Luminis: `http://localhost:8081/pages/docs/`.

## Probar el socket con Postman

1. Arranca la API (puerto `8081`) y entra a `http://localhost:8081/pages/registro/` para crear una cuenta, o usa una existente.
2. Desde el Center asigna un plan al cliente y, en el área del cliente, crea una API key en **Configuración → API keys** con los permisos `conectar` y `enviar`.
3. En Postman: **New → WebSocket** y conecta a:

```
ws://localhost:8081/socket?apikey=lm_TU_CLAVE&usuario=sensor-01
```

Al conectar recibirás el evento `ready` con la sesión y los límites del plan. Abre una segunda pestaña con `usuario=dashboard` y envía desde la primera:

```json
{ "type": "message", "destino": "dashboard", "mensaje": { "temp": 23.4, "hum": 61 }, "ref": "prueba-1" }
```

La primera pestaña recibe `sent` y `delivered`; la segunda recibe `message`. Para confirmar el procesado desde la segunda:

```json
{ "type": "seen", "mensajeId": "ID_RECIBIDO", "destino": "sensor-01" }
```

Ping: `{ "type": "ping", "ref": "p1" }` → responde `pong`.

## Protocolo WebSocket

Conexión: `ws(s)://host/<contexto>/socket?apikey=lm_xxx&usuario=ana`. También se acepta la key en el encabezado `X-Api-Key`.

Tramas:

- `{ "type":"message", "destino":"beto", "mensaje": <cualquier JSON o texto>, "ref":"opcional" }`
- `{ "type":"seen", "mensajeId":"...", "destino":"ana" }`
- `{ "type":"ping", "ref":"opcional" }`
- `{ "type":"presencia", "usuarios":["ana","beto"] }`

Eventos: `ready`, `message`, `sent`, `delivered`, `seen`, `desaparecido`, `desconocido`, `pong`, `presencia` y `error`.

Códigos de cierre: 4400 usuario inválido; 4401 API key inválida o revocada; 4402 sin plan vigente; 4403 permiso insuficiente; 4429 límite de conexiones.

## API REST

Las solicitudes autenticadas usan `X-Api-Key: lm_…`.

- `GET /api/luminis/cuenta/getById`: cuenta, plan, consumo, límites y permisos.
- `POST /api/luminis/mensajes/add`: publicar un evento con `origen`, `destino` y `mensaje`.
- `POST /api/luminis/presencia/getAll`: consultar usuarios conectados.
- `POST /api/luminis/presencia/getById`: consultar un usuario (`{ "usuario": "beto" }`).
- `DELETE /api/luminis/conexiones/delete`: desconectar un usuario.

Público: `GET /api/general/resumen/getRed` devuelve el pulso de la red (conexiones, eventos de hoy, tasa de entrega) que alimenta el globo del portal.

El panel usa JWT. El monitor se sirve desde `/api/mensajeria/monitor/getAll` y `/api/mensajeria/monitor/delete`.

## Estructura

- `API/Luminis/src/main/java/com/aarondevs/luminis`: módulos `Seguridad`, `Administracion`, `Clientes`, `Suscripciones`, `Mensajeria`, `General` y `Luminis` en `Controllers`, `Services`, `DAO`, `Models` y `Parameters`.
- `API/Luminis/src/main/webapp/pages`: `login`, `registro`, `center`, `customer` (pantallas en `pantallas/<opción>`), `comunes`, `files` (`img`, `languages`, `temes`, `paises`, `mapa`) y el motor del globo `globo.js`.
- `API/Luminis/src/main/resources/permisos`: catálogos de permisos de administración y clientes.

## Configuración

La configuración se carga desde `API/Luminis/opt/luminis/.env`. Incluye `DB_*`, `JWT_*`, `SEGURIDAD_*`, `LUMINIS_ADMIN_*` y `LUMINIS_SOCKET_*`. El servidor local usa el puerto 8081 y la base `luminis`. El archivo `.env` forma parte del proyecto y no debe ignorarse.

## Migraciones y ejecución

Requisitos: JDK 17, Maven y MariaDB. Flyway aplica `V1__estructura_inicial_luminis.sql` y `V2__catalogo_inicial_luminis.sql`.

Desde `API/Luminis`:

```bash
mvn -o -q compile
mvn -o test
mvn -o clean package
```

El artefacto es `target/luminis-1.0.0.war`. Despliega en Tomcat 11. Si Nginx actúa como proxy, conserva HTTP/1.1 y los encabezados Upgrade y Connection para WebSocket.

## Licencia

Este proyecto está bajo la licencia AaronDevs.
