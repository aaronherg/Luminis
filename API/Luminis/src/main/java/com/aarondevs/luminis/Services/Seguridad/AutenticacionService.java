package com.aarondevs.luminis.Services.Seguridad;

import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionCambiarContrasenaParameters;
import com.aarondevs.luminis.Parameters.Seguridad.AutenticacionLoginParameters;
import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.util.Map;

public interface AutenticacionService {
    Map<String, Object> autenticar(AutenticacionLoginParameters parametro);

    Map<String, Object> sesionActual(ContextoAutenticacion contexto);

    boolean cerrar(ContextoAutenticacion contexto);

    void cambiarContrasena(ContextoAutenticacion contexto, AutenticacionCambiarContrasenaParameters parametro);
}
