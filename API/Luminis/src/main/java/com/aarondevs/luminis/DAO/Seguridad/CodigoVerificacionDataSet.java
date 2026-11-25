package com.aarondevs.luminis.DAO.Seguridad;

import java.sql.Timestamp;
import java.util.Map;

public interface CodigoVerificacionDataSet {
    void crear(Long usuarioId, String proposito, String codigoHash, Timestamp expira);

    Map<String, Object> vigente(Long usuarioId, String proposito);

    void usar(Long codigoVerificacionId);

    void sumarIntento(Long codigoVerificacionId);
}
