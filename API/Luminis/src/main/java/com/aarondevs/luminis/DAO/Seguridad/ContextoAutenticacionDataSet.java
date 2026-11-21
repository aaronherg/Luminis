package com.aarondevs.luminis.DAO.Seguridad;

import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.util.Set;

public interface ContextoAutenticacionDataSet {
    ContextoAutenticacion resolver(String usuarioUuid, String tipo, String tokenJti);

    Set<String> accionesAdministracion(Long usuarioAdministracionId);

    Set<String> accionesCliente(Long usuarioClienteId);

    void registrarActividad(String tokenJti);
}
