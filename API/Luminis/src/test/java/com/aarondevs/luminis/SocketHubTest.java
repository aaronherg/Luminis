package com.aarondevs.luminis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aarondevs.luminis.DAO.Mensajeria.MetricaDataSet;
import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.aarondevs.luminis.Models.Suscripciones.DetalleSuscripcion;
import com.aarondevs.luminis.Models.Suscripciones.Suscripcion;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyActualizarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyAgregarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionAsignarParameters;
import com.aarondevs.luminis.Services.Mensajeria.ApiKeyService;
import com.aarondevs.luminis.Services.Mensajeria.ConsumoMensajeria;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

class SocketHubTest {
    private static final String CLAVE = "lm_valida";
    private static final String CLAVE_SIN_ENVIAR = "lm_lectura";
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final ObjectMapper json = new ObjectMapper();
    private SocketHub hub;
    private Long limitePeticiones;

    @BeforeEach
    void preparar() {
        limitePeticiones = null;
        ConsumoMensajeria consumo = new ConsumoMensajeria(new SuscripcionDePrueba(), new MetricasEnMemoria());
        hub = new SocketHub(new LlavesDePrueba(), consumo, new MetricasEnMemoria(), 1024, 1000, 1000, 20);
    }

    private Sesion conectar(String usuario) {
        return conectar(CLAVE, usuario);
    }

    private Sesion conectar(String clave, String usuario) {
        Sesion sesion = new Sesion("ws://localhost/socket?apikey=" + clave + "&usuario=" + usuario);
        hub.abrir(sesion);
        return sesion;
    }

    private void enviar(Sesion sesion, String texto) {
        hub.recibir(sesion, texto);
    }

    private List<JsonNode> eventos(Sesion sesion, String tipo) throws Exception {
        List<JsonNode> lista = new ArrayList<>();
        for (String texto : sesion.enviados) {
            JsonNode nodo = json.readTree(texto);
            if (tipo.equals(nodo.path("type").asText())) lista.add(nodo);
        }
        return lista;
    }

    @Test
    void entregaMensajesConEstadosYVisto() throws Exception {
        Sesion ana = conectar("ana");
        Sesion beto = conectar("beto");
        assertEquals(1, eventos(ana, "ready").size());
        enviar(ana, "{\"type\":\"message\",\"destino\":\"beto\",\"mensaje\":{\"texto\":\"hola\"},\"ref\":\"r1\"}");
        JsonNode recibido = eventos(beto, "message").get(0);
        assertEquals("ana", recibido.path("origen").asText());
        assertEquals("hola", recibido.path("mensaje").path("texto").asText());
        assertEquals("r1", eventos(ana, "sent").get(0).path("ref").asText());
        assertEquals(1, eventos(ana, "delivered").size());
        String mensajeId = recibido.path("mensajeId").asText();
        enviar(beto, "{\"type\":\"seen\",\"mensajeId\":\"" + mensajeId + "\",\"destino\":\"ana\"}");
        assertEquals(mensajeId, eventos(ana, "seen").get(0).path("mensajeId").asText());
    }

    @Test
    void destinoDesconocidoYColaOffline() throws Exception {
        Sesion ana = conectar("ana");
        enviar(ana, "{\"destino\":\"fantasma\",\"mensaje\":\"hola\"}");
        assertEquals(1, eventos(ana, "desconocido").size());
        Sesion beto = conectar("beto");
        hub.cerrar(beto);
        enviar(ana, "{\"destino\":\"beto\",\"mensaje\":\"te dejo esto\"}");
        assertEquals(0, eventos(ana, "delivered").size());
        Sesion betoDeNuevo = conectar("beto");
        assertEquals("te dejo esto", eventos(betoDeNuevo, "message").get(0).path("mensaje").asText());
        assertEquals(1, eventos(ana, "delivered").size());
    }

    @Test
    void validaLlavePermisosLimitesYFormato() throws Exception {
        Sesion invalida = conectar("lm_otra", "ana");
        assertEquals(4401, invalida.cierre.getCode());
        Sesion sinUsuario = conectar(CLAVE, "");
        assertEquals(4400, sinUsuario.cierre.getCode());
        Sesion lectura = conectar(CLAVE_SIN_ENVIAR, "lector");
        enviar(lectura, "{\"destino\":\"x\",\"mensaje\":\"hola\"}");
        assertEquals("SIN_PERMISO", eventos(lectura, "error").get(0).path("code").asText());
        Sesion ana = conectar("ana");
        enviar(ana, "no es json");
        assertEquals("FORMATO", eventos(ana, "error").get(0).path("code").asText());
        enviar(ana, "{\"type\":\"ping\",\"ref\":\"p\"}");
        assertEquals("p", eventos(ana, "pong").get(0).path("ref").asText());
    }

    @Test
    void respetaElLimiteDePeticionesDelPlan() throws Exception {
        limitePeticiones = 2L;
        Sesion ana = conectar("ana");
        conectar("beto");
        for (int i = 0; i < 3; i++) enviar(ana, "{\"destino\":\"beto\",\"mensaje\":\"m" + i + "\"}");
        assertEquals(2, eventos(ana, "delivered").size());
        assertEquals("LIMITE_PETICIONES", eventos(ana, "error").get(0).path("code").asText());
    }

    @Test
    void publicaDesdeLaApiYConsultaPresencia() throws Exception {
        Sesion beto = conectar("beto");
        Map<String, Object> resultado = hub.publicar(1L, "servidor", "beto", Map.of("aviso", "listo"), 20);
        assertEquals("entregado", resultado.get("estado"));
        assertEquals("servidor", eventos(beto, "message").get(0).path("origen").asText());
        assertTrue(hub.enLinea(1L, "beto"));
        assertFalse(hub.enLinea(1L, "nadie"));
        assertEquals(1, hub.desconectar(1L, "beto", "prueba"));
    }

    private class Sesion implements WebSocketSession {
        private final String id = "s" + SECUENCIA.incrementAndGet();
        private final URI uri;
        private final List<String> enviados = new ArrayList<>();
        private CloseStatus cierre;

        Sesion(String uri) {
            this.uri = URI.create(uri);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new HttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 5000);
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int limite) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int limite) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public synchronized void sendMessage(WebSocketMessage<?> mensaje) {
            if (mensaje instanceof TextMessage texto) enviados.add(texto.getPayload());
        }

        @Override
        public boolean isOpen() {
            return cierre == null;
        }

        @Override
        public void close() {
            close(CloseStatus.NORMAL);
        }

        @Override
        public void close(CloseStatus estado) {
            if (cierre != null) return;
            cierre = estado;
            hub.cerrar(this);
        }
    }

    private static class LlavesDePrueba implements ApiKeyService {
        @Override
        public ApiKey crear(ApiKeyAgregarParameters parametro, Long clienteId, String actor) {
            return null;
        }

        @Override
        public ApiKey actualizar(ApiKeyActualizarParameters parametro, String actor) {
            return null;
        }

        @Override
        public boolean revocar(Long apiKeyId, String actor) {
            return false;
        }

        @Override
        public ApiKey id(Long apiKeyId) {
            return null;
        }

        @Override
        public List<ApiKey> all(Long clienteId) {
            return List.of();
        }

        @Override
        public ApiKey autenticar(String clave, String permiso) {
            ApiKey llave = new ApiKey();
            llave.setApiKeyId(1L);
            llave.setClienteId(1L);
            llave.setClienteUuid("11111111-2222-3333-4444-555555555555");
            llave.setNombre("Prueba");
            if (CLAVE.equals(clave)) llave.setPermisos(String.join(",", PERMISOS));
            else if (CLAVE_SIN_ENVIAR.equals(clave)) llave.setPermisos("conectar");
            else throw new SecurityException("APIKEY NO VALIDA");
            if (!llave.getPermisos().contains(permiso)) throw new SecurityException("NO TIENES PERMISO PARA ESTA ACCION");
            return llave;
        }
    }

    private static class MetricasEnMemoria implements MetricaDataSet {
        @Override
        public void acumular(Long clienteId, LocalDate fecha, Map<String, Long> valores, int picoConexiones) {
        }

        @Override
        public Set<Long> apiKeysVigentes(Collection<Long> apiKeyIds) {
            return Set.copyOf(apiKeyIds);
        }
    }

    private class SuscripcionDePrueba implements SuscripcionService {
        @Override
        public Suscripcion asignar(SuscripcionAsignarParameters parametro, String actor, Long usuarioAdministracionId) {
            return null;
        }

        @Override
        public int cancelar(Long clienteId, String actor) {
            return 0;
        }

        @Override
        public Suscripcion cliente(Long clienteId) {
            return null;
        }

        @Override
        public Object pages(Integer page, Integer size, String buscar) {
            return null;
        }

        @Override
        public DetalleSuscripcion vigente(Long clienteId) {
            DetalleSuscripcion plan = new DetalleSuscripcion();
            plan.setPlanNombre("Prueba");
            plan.setPeticionesMes(limitePeticiones);
            plan.setRetencionSegundos(10);
            plan.setPermitirAdicionales(false);
            return plan;
        }

        @Override
        public Object historial(Long clienteId) {
            return List.of();
        }

        @Override
        public long peticionesUsadas(Long clienteId) {
            return 0;
        }

        @Override
        public Map<String, Object> consumo(Long clienteId) {
            return Map.of();
        }

        @Override
        public void renovarCadena(DetalleSuscripcion vencido, LocalDateTime limite) {
        }

        @Override
        public List<DetalleSuscripcion> pendientesRenovacion(LocalDateTime limite) {
            return List.of();
        }
    }
}
