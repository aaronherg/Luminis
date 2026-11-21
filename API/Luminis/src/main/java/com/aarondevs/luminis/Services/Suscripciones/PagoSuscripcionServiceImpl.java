package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.DAO.Suscripciones.PagoSuscripcionDataSet;
import com.aarondevs.luminis.Models.Suscripciones.PagoSuscripcion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoSuscripcionServiceImpl implements PagoSuscripcionService {
    private static final Set<String> METODOS = Set.of("Efectivo", "Transferencia", "Tarjeta", "Deposito", "Otro");

    private final PagoSuscripcionDataSet datos;

    public PagoSuscripcionServiceImpl(PagoSuscripcionDataSet datos) {
        this.datos = datos;
    }

    @Override
    @Transactional
    public PagoSuscripcion registrar(Long pagoSuscripcionId, BigDecimal monto, String metodo, String referencia, String observacion,
                                     String actor, Long usuarioAdministracionId) {
        if (pagoSuscripcionId == null) throw new IllegalArgumentException("pagoSuscripcionId es requerido");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El monto debe ser mayor que cero");
        if (monto.scale() > 2) throw new IllegalArgumentException("El monto admite hasta 2 decimales");
        String metodoPago = metodo == null || metodo.isBlank() ? "Efectivo" : metodo.trim();
        if (!METODOS.contains(metodoPago)) throw new IllegalArgumentException("metodo debe ser " + String.join(", ", METODOS));
        if (referencia != null && referencia.length() > 120) throw new IllegalArgumentException("La referencia admite hasta 120 caracteres");
        if (observacion != null && observacion.length() > 300) throw new IllegalArgumentException("La observación admite hasta 300 caracteres");
        PagoSuscripcion pago = datos.findById(pagoSuscripcionId);
        if (pago == null) throw new IllegalArgumentException("El cargo indicado no existe");
        if ("Pagado".equals(pago.getEstado())) throw new IllegalArgumentException("El cargo ya está saldado");
        BigDecimal pendiente = pago.getMontoTotal().subtract(pago.getMontoPagado()).setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(pendiente) > 0) {
            throw new IllegalArgumentException("El monto excede el pendiente de " + pago.getMonedaSimbolo() + " " + pendiente.toPlainString());
        }
        datos.createDetalle(pagoSuscripcionId, monto, metodoPago, limpiar(referencia), limpiar(observacion), usuarioAdministracionId, actor);
        datos.aplicarMonto(pagoSuscripcionId, monto, actor);
        return id(pagoSuscripcionId);
    }

    @Override
    public Object pages(Long clienteId, String estado, String buscar, Integer page, Integer size) {
        return datos.findAllPages(clienteId, estado, buscar, page, size);
    }

    @Override
    public PagoSuscripcion id(Long pagoSuscripcionId) {
        PagoSuscripcion pago = datos.findById(pagoSuscripcionId);
        if (pago != null) pago.setDetalles(datos.findDetalles(pagoSuscripcionId));
        return pago;
    }

    @Override
    public Long clienteDeSuscripcion(Long suscripcionId) {
        return datos.clienteDeSuscripcion(suscripcionId);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
