package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;
import com.aarondevs.luminis.Parameters.Administracion.ServicioSmtpParameters;
import com.aarondevs.luminis.Parameters.Administracion.ServicioWhatsappParameters;

public interface ServicioNotificacionService {
    ConfiguracionNotificacion obtener();

    ConfiguracionNotificacion guardarSmtp(ServicioSmtpParameters parametro, String actor);

    void probarSmtp(String destino);

    ConfiguracionNotificacion guardarWhatsapp(ServicioWhatsappParameters parametro, String actor);

    void probarWhatsapp(String destino);
}
