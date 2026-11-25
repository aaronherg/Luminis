package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;

public interface ConfiguracionNotificacionDataSet {
    ConfiguracionNotificacion obtener();

    void guardarSmtp(boolean habilitado, String host, String puerto, String seguridad, String usuario,
                     String contrasenaCifrada, String remitente, String nombreRemitente, String actor);

    void guardarWhatsapp(boolean habilitado, String dispositivo, String apiKeyCifrada, String telefonoPrueba, String actor);
}
