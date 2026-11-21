package com.aarondevs.luminis.Services.Mensajeria;

import com.aarondevs.luminis.DAO.Mensajeria.MetricaDataSet;
import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Component
public class SocketHub {
    public static final String VERSION_PROTOCOLO = "2";
    private static final Logger log = LoggerFactory.getLogger(SocketHub.class);
    private static final Pattern USUARIO_VALIDO = Pattern.compile("^[A-Za-z0-9._@:-]{1,80}$");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final long PONG_MAXIMO_MS = 75_000;

    private final ApiKeyService apiKeys;
    private final ConsumoMensajeria consumo;
    private final MetricaDataSet metricas;
    private final ObjectMapper json = new ObjectMapper();
    private final int maximoKb;
    private final int rafagaMaxima;
    private final double mensajesPorSegundo;
    private final int conexionesPorUsuario;

    private final AtomicLong eventosProcesados = new AtomicLong();
    private final ConcurrentHashMap<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Sesion>> usuarios = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> conocidos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> desconectados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Pendiente>> colas = new ConcurrentHashMap<>();

    public SocketHub(ApiKeyService apiKeys, ConsumoMensajeria consumo, MetricaDataSet metricas,
                     @Value("${app.luminis.socket.max-kb:1024}") int maximoKb,
                     @Value("${app.luminis.socket.mensajes-por-segundo:30}") double mensajesPorSegundo,
                     @Value("${app.luminis.socket.rafaga:60}") int rafagaMaxima,
                     @Value("${app.luminis.socket.conexiones-por-usuario:20}") int conexionesPorUsuario) {
        this.apiKeys = apiKeys;
        this.consumo = consumo;
        this.metricas = metricas;
        this.maximoKb = maximoKb;
        this.mensajesPorSegundo = mensajesPorSegundo;
        this.rafagaMaxima = rafagaMaxima;
        this.conexionesPorUsuario = conexionesPorUsuario;
    }

    public static final class Sesion {
        private final String id;
        private final WebSocketSession ws;
        private final Long clienteId;
        private final String clienteUuid;
        private final String usuario;
        private final Long apiKeyId;
        private final String apiKeyNombre;
        private final Set<String> permisos;
        private final Instant conectado = Instant.now();
        private final String ip;
        private final AtomicLong enviados = new AtomicLong();
        private final AtomicLong recibidos = new AtomicLong();
        private volatile long ultimoPong = System.currentTimeMillis();
        private volatile long ultimaActividad = System.currentTimeMillis();
        private double fichas;
        private long ultimoRelleno = System.nanoTime();

        private Sesion(WebSocketSession ws, ApiKey llave, String usuario, String ip, int rafaga) {
            this.id = ws.getId();
            this.ws = ws;
            this.clienteId = llave.getClienteId();
            this.clienteUuid = llave.getClienteUuid();
            this.usuario = usuario;
            this.apiKeyId = llave.getApiKeyId();
            this.apiKeyNombre = llave.getNombre();
            this.permisos = new HashSet<>(Arrays.asList(llave.getPermisos().split(",")));
            this.ip = ip;
            this.fichas = rafaga;
        }

        private synchronized boolean tomarFicha(double porSegundo, int rafaga) {
            long ahora = System.nanoTime();
            fichas = Math.min(rafaga, fichas + (ahora - ultimoRelleno) / 1_000_000_000.0 * porSegundo);
            ultimoRelleno = ahora;
            if (fichas < 1) return false;
            fichas -= 1;
            return true;
        }
    }

    private record Pendiente(Map<String, Object> evento, long expira, String claveOrigen, String mensajeId, String destino, boolean esMensaje) {
    }

    private static String clave(Long clienteId, String usuario) {
        return clienteId + ":" + usuario;
    }

    private static String nuevoId() {
        byte[] bytes = new byte[8];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static Map<String, Object> evento(Object... pares) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            if (pares[i + 1] != null) mapa.put(String.valueOf(pares[i]), pares[i + 1]);
        }
        return mapa;
    }

    public void abrir(WebSocketSession original) {
        Map<String, String> parametros = parametros(original.getUri());
        String clave = parametros.getOrDefault("apikey", primerEncabezado(original, "X-Api-Key"));
        String usuario = parametros.getOrDefault("usuario", "").trim();
        if (clave == null || clave.isBlank()) {
            rechazar(original, 4401, "API_KEY_REQUERIDA", "Conéctese con ?apikey=<su API key>&usuario=<id del usuario>");
            return;
        }
        if (!USUARIO_VALIDO.matcher(usuario).matches()) {
            rechazar(original, 4400, "USUARIO_INVALIDO", "El parámetro usuario es requerido: 1 a 80 caracteres (letras, números, . _ @ : -)");
            return;
        }
        ApiKey llave;
        try {
            llave = apiKeys.autenticar(clave, "conectar");
        } catch (SecurityException error) {
            boolean invalida = "APIKEY NO VALIDA".equals(error.getMessage());
            rechazar(original, invalida ? 4401 : 4403, invalida ? "API_KEY_INVALIDA" : "SIN_PERMISO",
                    invalida ? "La API key no es válida o está suspendida" : "La API key no tiene el permiso conectar");
            return;
        }
        ConsumoMensajeria.Limites limites = consumo.limites(llave.getClienteId());
        if (!limites.vigente()) {
            rechazar(original, 4402, "SIN_PLAN", "La cuenta no tiene un plan vigente");
            return;
        }
        long abiertas = sesiones.values().stream().filter(s -> s.clienteId.equals(llave.getClienteId())).count();
        if (limites.conexiones() != null && abiertas >= limites.conexiones()) {
            consumo.sumar(llave.getClienteId(), "rechazados", 1);
            rechazar(original, 4429, "LIMITE_CONEXIONES", "Se alcanzó el máximo de " + limites.conexiones() + " conexiones simultáneas del plan");
            return;
        }
        String claveUsuario = clave(llave.getClienteId(), usuario);
        Set<Sesion> delUsuario = usuarios.computeIfAbsent(claveUsuario, k -> new CopyOnWriteArraySet<>());
        if (delUsuario.size() >= conexionesPorUsuario) {
            rechazar(original, 4429, "LIMITE_CONEXIONES_USUARIO", "El usuario ya tiene " + conexionesPorUsuario + " conexiones abiertas");
            return;
        }
        WebSocketSession ws = new ConcurrentWebSocketSessionDecorator(original, 10_000, 512 * 1024);
        Sesion sesion = new Sesion(ws, llave, usuario, ip(original), rafagaMaxima);
        sesiones.put(sesion.id, sesion);
        delUsuario.add(sesion);
        conocidos.put(claveUsuario, Boolean.TRUE);
        desconectados.remove(claveUsuario);
        consumo.sumar(llave.getClienteId(), "conexiones", 1);
        consumo.registrarPico(llave.getClienteId(), (int) abiertas + 1);
        enviar(sesion, evento("type", "ready", "usuario", usuario, "sesion", sesion.id, "protocolo", VERSION_PROTOCOLO,
                "limites", evento("plan", limites.plan(), "peticionesMes", limites.peticionesMes(), "conexiones", limites.conexiones(),
                        "mensajeKb", kilobytesPermitidos(limites), "retencionSegundos", limites.retencionSegundos())));
        entregarPendientes(claveUsuario, sesion);
    }

    public void pong(WebSocketSession ws) {
        Sesion sesion = sesiones.get(ws.getId());
        if (sesion != null) sesion.ultimoPong = System.currentTimeMillis();
    }

    public void recibir(WebSocketSession ws, String texto) {
        Sesion sesion = sesiones.get(ws.getId());
        if (sesion == null) return;
        long ahora = System.currentTimeMillis();
        sesion.ultimaActividad = ahora;
        sesion.ultimoPong = ahora;
        int bytes = texto.getBytes(StandardCharsets.UTF_8).length;
        ConsumoMensajeria.Limites limites = consumo.limites(sesion.clienteId);
        if (bytes > kilobytesPermitidos(limites) * 1024) {
            consumo.sumar(sesion.clienteId, "rechazados", 1);
            error(sesion, "MENSAJE_GRANDE", "El mensaje supera el máximo de " + kilobytesPermitidos(limites) + " KB del plan", null);
            return;
        }
        if (!sesion.tomarFicha(mensajesPorSegundo, rafagaMaxima)) {
            consumo.sumar(sesion.clienteId, "rechazados", 1);
            error(sesion, "RATE_LIMIT", "Demasiados mensajes por segundo; reduzca la frecuencia", null);
            return;
        }
        JsonNode datos;
        try {
            datos = json.readTree(texto);
        } catch (Exception errorFormato) {
            error(sesion, "FORMATO", "El mensaje debe ser un JSON válido", null);
            return;
        }
        if (datos == null || !datos.isObject()) {
            error(sesion, "FORMATO", "El mensaje debe ser un objeto JSON", null);
            return;
        }
        String tipo = datos.path("type").asText("message");
        String ref = datos.hasNonNull("ref") ? datos.get("ref").asText() : null;
        switch (tipo) {
            case "ping" -> enviar(sesion, evento("type", "pong", "ref", ref, "hora", Instant.now().toString()));
            case "seen" -> visto(sesion, datos, ref);
            case "presencia" -> presencia(sesion, datos, ref);
            case "message" -> mensaje(sesion, datos, ref, bytes);
            default -> error(sesion, "TIPO_DESCONOCIDO", "Tipo de mensaje no soportado: " + tipo, ref);
        }
    }

    private void mensaje(Sesion sesion, JsonNode datos, String ref, int bytes) {
        if (!sesion.permisos.contains("enviar")) {
            error(sesion, "SIN_PERMISO", "La API key no tiene el permiso enviar", ref);
            return;
        }
        String destino = datos.path("destino").asText("").trim();
        JsonNode contenido = datos.get("mensaje");
        if (!USUARIO_VALIDO.matcher(destino).matches()) {
            error(sesion, "DESTINO_INVALIDO", "El destino es requerido: 1 a 80 caracteres (letras, números, . _ @ : -)", ref);
            return;
        }
        if (contenido == null || contenido.isNull() || (contenido.isTextual() && contenido.asText().isEmpty())) {
            error(sesion, "MENSAJE_VACIO", "El campo mensaje es requerido", ref);
            return;
        }
        if (!consumo.consumirPeticion(sesion.clienteId)) {
            consumo.sumar(sesion.clienteId, "rechazados", 1);
            error(sesion, "LIMITE_PETICIONES", "Se alcanzó el límite de peticiones del plan", ref);
            return;
        }
        sesion.enviados.incrementAndGet();
        consumo.sumar(sesion.clienteId, "bytes", bytes);
        entregar(sesion.clienteId, sesion.usuario, destino, json.convertValue(contenido, Object.class), ref);
    }

    public Map<String, Object> publicar(Long clienteId, String origen, String destino, Object contenido, int bytes) {
        if (!USUARIO_VALIDO.matcher(origen == null ? "" : origen).matches()) throw new IllegalArgumentException("origen no es válido");
        if (!USUARIO_VALIDO.matcher(destino == null ? "" : destino).matches()) throw new IllegalArgumentException("destino no es válido");
        if (contenido == null) throw new IllegalArgumentException("mensaje es requerido");
        ConsumoMensajeria.Limites limites = consumo.limites(clienteId);
        if (bytes > kilobytesPermitidos(limites) * 1024) {
            consumo.sumar(clienteId, "rechazados", 1);
            throw new IllegalArgumentException("El mensaje supera el máximo de " + kilobytesPermitidos(limites) + " KB del plan");
        }
        if (!consumo.consumirPeticion(clienteId)) {
            consumo.sumar(clienteId, "rechazados", 1);
            throw new LimiteException("Se alcanzó el límite de peticiones del plan");
        }
        consumo.sumar(clienteId, "bytes", bytes);
        return entregar(clienteId, origen, destino, contenido, null);
    }

    private Map<String, Object> entregar(Long clienteId, String origen, String destino, Object contenido, String ref) {
        String mensajeId = nuevoId();
        String claveOrigen = clave(clienteId, origen);
        String claveDestino = clave(clienteId, destino);
        consumo.sumar(clienteId, "mensajes", 1);
        eventosProcesados.incrementAndGet();
        if (!conocidos.containsKey(claveDestino)) {
            consumo.sumar(clienteId, "desaparecidos", 1);
            notificar(claveOrigen, evento("type", "desconocido", "mensajeId", mensajeId, "destino", destino, "ref", ref));
            return evento("mensajeId", mensajeId, "estado", "desconocido");
        }
        int retencion = consumo.limites(clienteId).retencionSegundos();
        Long desde = desconectados.get(claveDestino);
        if (desde != null && System.currentTimeMillis() - desde > retencion * 1000L) {
            consumo.sumar(clienteId, "desaparecidos", 1);
            notificar(claveOrigen, evento("type", "desaparecido", "mensajeId", mensajeId, "destino", destino, "ref", ref));
            return evento("mensajeId", mensajeId, "estado", "desaparecido");
        }
        notificar(claveOrigen, evento("type", "sent", "mensajeId", mensajeId, "destino", destino, "ref", ref));
        Map<String, Object> carga = evento("type", "message", "mensajeId", mensajeId, "origen", origen, "destino", destino, "mensaje", contenido,
                "fecha", Instant.now().toString());
        Set<Sesion> enLinea = usuarios.get(claveDestino);
        if (enLinea != null && !enLinea.isEmpty()) {
            enLinea.forEach(s -> {
                enviar(s, carga);
                s.recibidos.incrementAndGet();
            });
            consumo.sumar(clienteId, "entregados", 1);
            notificar(claveOrigen, evento("type", "delivered", "mensajeId", mensajeId, "destino", destino, "ref", ref));
            return evento("mensajeId", mensajeId, "estado", "entregado");
        }
        colas.computeIfAbsent(claveDestino, k -> new ConcurrentLinkedQueue<>())
                .add(new Pendiente(carga, System.currentTimeMillis() + retencion * 1000L, claveOrigen, mensajeId, destino, true));
        return evento("mensajeId", mensajeId, "estado", "en_cola");
    }

    private void visto(Sesion sesion, JsonNode datos, String ref) {
        String mensajeId = datos.path("mensajeId").asText("").trim();
        String destino = datos.path("destino").asText("").trim();
        if (mensajeId.isEmpty() || !USUARIO_VALIDO.matcher(destino).matches()) {
            error(sesion, "FORMATO", "seen requiere mensajeId y destino", ref);
            return;
        }
        consumo.sumar(sesion.clienteId, "vistos", 1);
        Map<String, Object> carga = evento("type", "seen", "mensajeId", mensajeId, "origen", sesion.usuario);
        String claveDestino = clave(sesion.clienteId, destino);
        Set<Sesion> enLinea = usuarios.get(claveDestino);
        if (enLinea != null && !enLinea.isEmpty()) {
            enLinea.forEach(s -> enviar(s, carga));
            return;
        }
        int retencion = consumo.limites(sesion.clienteId).retencionSegundos();
        colas.computeIfAbsent(claveDestino, k -> new ConcurrentLinkedQueue<>())
                .add(new Pendiente(carga, System.currentTimeMillis() + Math.max(retencion, 10) * 1000L, null, mensajeId, destino, false));
    }

    private void presencia(Sesion sesion, JsonNode datos, String ref) {
        if (!sesion.permisos.contains("consultar_presencia")) {
            error(sesion, "SIN_PERMISO", "La API key no tiene el permiso consultar_presencia", ref);
            return;
        }
        Map<String, Object> estados = new LinkedHashMap<>();
        int contador = 0;
        for (JsonNode usuario : datos.path("usuarios")) {
            if (++contador > 200) break;
            String nombre = usuario.asText("");
            if (USUARIO_VALIDO.matcher(nombre).matches()) estados.put(nombre, enLinea(sesion.clienteId, nombre));
        }
        enviar(sesion, evento("type", "presencia", "ref", ref, "usuarios", estados));
    }

    private void entregarPendientes(String claveUsuario, Sesion sesion) {
        ConcurrentLinkedQueue<Pendiente> cola = colas.remove(claveUsuario);
        if (cola == null) return;
        for (Pendiente pendiente : cola) {
            enviar(sesion, pendiente.evento());
            if (!pendiente.esMensaje()) continue;
            sesion.recibidos.incrementAndGet();
            consumo.sumar(sesion.clienteId, "entregados", 1);
            notificar(pendiente.claveOrigen(), evento("type", "delivered", "mensajeId", pendiente.mensajeId(), "destino", pendiente.destino()));
        }
    }

    public void cerrar(WebSocketSession ws) {
        Sesion sesion = sesiones.remove(ws.getId());
        if (sesion == null) return;
        String claveUsuario = clave(sesion.clienteId, sesion.usuario);
        usuarios.computeIfPresent(claveUsuario, (k, conjunto) -> {
            conjunto.remove(sesion);
            if (conjunto.isEmpty()) {
                desconectados.put(claveUsuario, System.currentTimeMillis());
                return null;
            }
            return conjunto;
        });
    }

    private void notificar(String claveUsuario, Map<String, Object> carga) {
        if (claveUsuario == null) return;
        Set<Sesion> enLinea = usuarios.get(claveUsuario);
        if (enLinea != null) enLinea.forEach(s -> enviar(s, carga));
    }

    private void error(Sesion sesion, String codigo, String mensaje, String ref) {
        enviar(sesion, evento("type", "error", "code", codigo, "mensaje", mensaje, "ref", ref));
    }

    private void enviar(Sesion sesion, Map<String, Object> carga) {
        if (!sesion.ws.isOpen()) return;
        try {
            sesion.ws.sendMessage(new TextMessage(json.writeValueAsString(carga)));
        } catch (Exception error) {
            log.debug("No se pudo enviar a la sesión {}: {}", sesion.id, error.getMessage());
            cerrarSilencioso(sesion.ws, CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    private void rechazar(WebSocketSession ws, int codigo, String error, String mensaje) {
        try {
            if (ws.isOpen()) ws.sendMessage(new TextMessage(json.writeValueAsString(evento("type", "error", "code", error, "mensaje", mensaje))));
        } catch (IOException ignorado) {
            log.debug("No se pudo notificar el rechazo: {}", ignorado.getMessage());
        }
        cerrarSilencioso(ws, new CloseStatus(codigo, error));
    }

    private void cerrarSilencioso(WebSocketSession ws, CloseStatus estado) {
        try {
            if (ws.isOpen()) ws.close(estado);
        } catch (IOException error) {
            log.debug("Error al cerrar la sesión {}: {}", ws.getId(), error.getMessage());
        }
    }

    private int kilobytesPermitidos(ConsumoMensajeria.Limites limites) {
        return limites.kilobytesMensaje() == null ? maximoKb : Math.min(limites.kilobytesMensaje(), maximoKb);
    }

    @Scheduled(fixedDelay = 1_000)
    public void limpiar() {
        long ahora = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, ConcurrentLinkedQueue<Pendiente>>> it = colas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ConcurrentLinkedQueue<Pendiente>> entrada = it.next();
            entrada.getValue().removeIf(p -> {
                if (p.expira() > ahora) return false;
                if (p.esMensaje()) {
                    consumo.sumar(clienteDe(entrada.getKey()), "desaparecidos", 1);
                    notificar(p.claveOrigen(), evento("type", "desaparecido", "mensajeId", p.mensajeId(), "destino", p.destino()));
                }
                return true;
            });
            if (entrada.getValue().isEmpty()) colas.remove(entrada.getKey(), entrada.getValue());
        }
        desconectados.entrySet().removeIf(entrada -> {
            int retencion = consumo.limites(clienteDe(entrada.getKey())).retencionSegundos();
            if (ahora - entrada.getValue() <= Math.max(retencion, 10) * 1000L) return false;
            if (!usuarios.containsKey(entrada.getKey())) conocidos.remove(entrada.getKey());
            return true;
        });
    }

    @Scheduled(fixedDelay = 25_000, initialDelay = 25_000)
    public void latido() {
        long ahora = System.currentTimeMillis();
        for (Sesion sesion : sesiones.values()) {
            if (ahora - sesion.ultimoPong > PONG_MAXIMO_MS) {
                cerrarSilencioso(sesion.ws, CloseStatus.SESSION_NOT_RELIABLE);
                continue;
            }
            try {
                if (sesion.ws.isOpen()) sesion.ws.sendMessage(new PingMessage());
            } catch (Exception error) {
                cerrarSilencioso(sesion.ws, CloseStatus.SESSION_NOT_RELIABLE);
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void revalidar() {
        Set<Long> llaves = new HashSet<>();
        sesiones.values().forEach(s -> llaves.add(s.apiKeyId));
        Set<Long> vigentes;
        try {
            vigentes = metricas.apiKeysVigentes(llaves);
        } catch (Exception error) {
            log.warn("No se pudieron revalidar las API keys: {}", error.getMessage());
            return;
        }
        for (Sesion sesion : sesiones.values()) {
            if (!vigentes.contains(sesion.apiKeyId)) {
                rechazar(sesion.ws, 4401, "API_KEY_REVOCADA", "La API key fue revocada o la cuenta está suspendida");
            } else if (!consumo.limites(sesion.clienteId).vigente()) {
                rechazar(sesion.ws, 4402, "SIN_PLAN", "La cuenta ya no tiene un plan vigente");
            }
        }
    }

    @PreDestroy
    public void apagar() {
        sesiones.values().forEach(s -> cerrarSilencioso(s.ws, CloseStatus.SERVICE_RESTARTED));
    }

    private Long clienteDe(String claveUsuario) {
        return Long.valueOf(claveUsuario.substring(0, claveUsuario.indexOf(':')));
    }

    public boolean enLinea(Long clienteId, String usuario) {
        Set<Sesion> conjunto = usuarios.get(clave(clienteId, usuario));
        return conjunto != null && !conjunto.isEmpty();
    }

    public int conexionesDe(Long clienteId, String usuario) {
        Set<Sesion> conjunto = usuarios.get(clave(clienteId, usuario));
        return conjunto == null ? 0 : conjunto.size();
    }

    public List<Map<String, Object>> sesiones(Long clienteId) {
        List<Map<String, Object>> lista = new ArrayList<>();
        sesiones.values().stream()
                .filter(s -> clienteId == null || s.clienteId.equals(clienteId))
                .sorted(Comparator.comparing((Sesion s) -> s.conectado).reversed())
                .limit(1000)
                .forEach(s -> lista.add(evento("sesion", s.id, "clienteId", s.clienteId, "clienteUuid", s.clienteUuid, "usuario", s.usuario,
                        "apiKey", s.apiKeyNombre, "ip", s.ip, "conectado", s.conectado.toString(), "enviados", s.enviados.get(),
                        "recibidos", s.recibidos.get(), "ultimaActividad", Instant.ofEpochMilli(s.ultimaActividad).toString())));
        return lista;
    }

    public List<String> usuariosEnLinea(Long clienteId, int limite) {
        String prefijo = clienteId + ":";
        return usuarios.keySet().stream().filter(k -> k.startsWith(prefijo)).map(k -> k.substring(prefijo.length())).sorted().limit(limite).toList();
    }

    public Map<String, Object> resumen(Long clienteId) {
        Collection<Sesion> todas = sesiones.values();
        long conexiones = todas.stream().filter(s -> clienteId == null || s.clienteId.equals(clienteId)).count();
        String prefijo = clienteId + ":";
        long enLinea = usuarios.keySet().stream().filter(k -> clienteId == null || k.startsWith(prefijo)).count();
        long enCola = colas.entrySet().stream().filter(e -> clienteId == null || e.getKey().startsWith(prefijo))
                .mapToLong(e -> e.getValue().stream().filter(Pendiente::esMensaje).count()).sum();
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("conexiones", conexiones);
        resultado.put("usuarios", enLinea);
        resultado.put("enCola", enCola);
        if (clienteId == null) resultado.put("clientes", todas.stream().map(s -> s.clienteId).distinct().count());
        return resultado;
    }

    public long eventosProcesados() {
        return eventosProcesados.get();
    }

    public int desconectar(Long clienteId, String usuario, String motivo) {
        Set<Sesion> conjunto = usuarios.get(clave(clienteId, usuario));
        if (conjunto == null) return 0;
        int total = conjunto.size();
        conjunto.forEach(s -> rechazar(s.ws, 4000, "DESCONECTADO", motivo));
        return total;
    }

    public boolean cerrarSesion(Long clienteId, String sesionId, String motivo) {
        Sesion sesion = sesiones.get(sesionId);
        if (sesion == null || (clienteId != null && !sesion.clienteId.equals(clienteId))) return false;
        rechazar(sesion.ws, 4000, "DESCONECTADO", motivo);
        return true;
    }

    private Map<String, String> parametros(URI uri) {
        Map<String, String> resultado = new HashMap<>();
        if (uri == null || uri.getRawQuery() == null) return resultado;
        for (String par : uri.getRawQuery().split("&")) {
            int igual = par.indexOf('=');
            if (igual <= 0) continue;
            resultado.put(URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8),
                    URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8));
        }
        return resultado;
    }

    private String primerEncabezado(WebSocketSession ws, String nombre) {
        List<String> valores = ws.getHandshakeHeaders().get(nombre);
        return valores == null || valores.isEmpty() ? null : valores.get(0);
    }

    private String ip(WebSocketSession ws) {
        String reenviada = primerEncabezado(ws, "X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) return reenviada.split(",")[0].trim();
        return ws.getRemoteAddress() == null ? null : ws.getRemoteAddress().getAddress().getHostAddress();
    }

    public static class LimiteException extends RuntimeException {
        public LimiteException(String mensaje) {
            super(mensaje);
        }
    }
}
