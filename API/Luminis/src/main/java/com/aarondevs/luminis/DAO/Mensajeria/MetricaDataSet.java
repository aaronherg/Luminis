package com.aarondevs.luminis.DAO.Mensajeria;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MetricaDataSet {
    void acumular(Long clienteId, LocalDate fecha, Map<String, Long> valores, int picoConexiones);

    Set<Long> apiKeysVigentes(Collection<Long> apiKeyIds);
}
