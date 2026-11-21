package com.aarondevs.luminis.Services.Mensajeria;

import com.aarondevs.luminis.DAO.Mensajeria.MetricaDAO;
import com.aarondevs.luminis.DAO.Mensajeria.MetricaDataSet;
import com.aarondevs.luminis.Models.Suscripciones.DetalleSuscripcion;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConsumoMensajeria {
    private static final Logger log = LoggerFactory.getLogger(ConsumoMensajeria.class);
    private static final long VIGENCIA_LIMITES_MS = 60_000;

    private final SuscripcionService suscripciones;
    private final MetricaDataSet metricas;
    private final ConcurrentHashMap<Long, Map<String, LongAdder>> pendientes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> picos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Limites> limites = new ConcurrentHashMap<>();

    public ConsumoMensajeria(SuscripcionService suscripciones, MetricaDataSet metricas) {
        this.suscripciones = suscripciones;
        this.metricas = metricas;
    }

    public static final class Limites {
        private volatile boolean vigente;
        private volatile String plan;
        private volatile Long peticionesMes;
        private volatile Integer conexiones;
        private volatile Integer kilobytesMensaje;
        private volatile int retencionSegundos = 10;
        private volatile boolean adicionales;
        private final AtomicLong usadas = new AtomicLong();
        private volatile long cargado;

        public boolean vigente() {
            return vigente;
        }

        public String plan() {
            return plan;
        }

        public Long peticionesMes() {
            return peticionesMes;
        }

        public Integer conexiones() {
            return conexiones;
        }

        public Integer kilobytesMensaje() {
            return kilobytesMensaje;
        }

        public int retencionSegundos() {
            return retencionSegundos;
        }

        public boolean adicionales() {
            return adicionales;
        }

        public long usadas() {
            return usadas.get();
        }
    }

    public Limites limites(Long clienteId) {
        Limites actual = limites.get(clienteId);
        if (actual != null && System.currentTimeMillis() - actual.cargado < VIGENCIA_LIMITES_MS) return actual;
        Limites nuevo = actual == null ? new Limites() : actual;
        cargar(clienteId, nuevo);
        limites.put(clienteId, nuevo);
        return nuevo;
    }

    private void cargar(Long clienteId, Limites destino) {
        DetalleSuscripcion vigente = suscripciones.vigente(clienteId);
        destino.vigente = vigente != null;
        destino.plan = vigente == null ? null : vigente.getPlanNombre();
        destino.peticionesMes = vigente == null ? null : vigente.getPeticionesMes();
        destino.conexiones = vigente == null ? null : vigente.getConexionesSimultaneas();
        destino.kilobytesMensaje = vigente == null ? null : vigente.getTamanoMaximoMensajeKb();
        destino.retencionSegundos = vigente == null || vigente.getRetencionSegundos() == null ? 10 : vigente.getRetencionSegundos();
        destino.adicionales = vigente != null && Boolean.TRUE.equals(vigente.getPermitirAdicionales());
        long enBase = vigente == null ? 0 : suscripciones.peticionesUsadas(clienteId);
        destino.usadas.set(enBase + pendiente(clienteId, "peticiones"));
        destino.cargado = System.currentTimeMillis();
    }

    public void invalidar(Long clienteId) {
        limites.remove(clienteId);
    }

    public boolean consumirPeticion(Long clienteId) {
        Limites actual = limites(clienteId);
        if (!actual.vigente) return false;
        if (actual.peticionesMes != null && !actual.adicionales && actual.usadas.get() >= actual.peticionesMes) return false;
        actual.usadas.incrementAndGet();
        sumar(clienteId, "peticiones", 1);
        return true;
    }

    public void sumar(Long clienteId, String campo, long cantidad) {
        if (cantidad == 0) return;
        pendientes.computeIfAbsent(clienteId, id -> new ConcurrentHashMap<>()).computeIfAbsent(campo, c -> new LongAdder()).add(cantidad);
    }

    public void registrarPico(Long clienteId, int conexiones) {
        picos.computeIfAbsent(clienteId, id -> new AtomicInteger()).accumulateAndGet(conexiones, Math::max);
    }

    private long pendiente(Long clienteId, String campo) {
        Map<String, LongAdder> contadores = pendientes.get(clienteId);
        LongAdder contador = contadores == null ? null : contadores.get(campo);
        return contador == null ? 0 : contador.sum();
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void volcar() {
        LocalDate hoy = LocalDate.now();
        for (Map.Entry<Long, Map<String, LongAdder>> entrada : pendientes.entrySet()) {
            Map<String, Long> valores = new HashMap<>();
            boolean hayDatos = false;
            for (String campo : MetricaDAO.CAMPOS) {
                LongAdder contador = entrada.getValue().get(campo);
                long valor = contador == null ? 0 : contador.sumThenReset();
                valores.put(campo, valor);
                hayDatos |= valor != 0;
            }
            AtomicInteger pico = picos.get(entrada.getKey());
            int valorPico = pico == null ? 0 : pico.getAndSet(0);
            if (!hayDatos && valorPico == 0) continue;
            try {
                metricas.acumular(entrada.getKey(), hoy, valores, valorPico);
            } catch (Exception error) {
                valores.forEach((campo, valor) -> sumar(entrada.getKey(), campo, valor));
                registrarPico(entrada.getKey(), valorPico);
                log.warn("No se pudieron guardar las métricas del cliente {}: {}", entrada.getKey(), error.getMessage());
            }
        }
    }

    @PreDestroy
    public void alCerrar() {
        volcar();
    }
}
