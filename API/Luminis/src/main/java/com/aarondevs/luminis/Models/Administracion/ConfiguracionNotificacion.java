package com.aarondevs.luminis.Models.Administracion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class ConfiguracionNotificacion {
    private Long configuracionNotificacionId;
    private Boolean smtpHabilitado;
    private String smtpHost;
    private String smtpPuerto;
    private String smtpSeguridad;
    private String smtpUsuario;
    @JsonIgnore
    private String smtpContrasena;
    private String smtpRemitente;
    private String smtpNombreRemitente;
    private Boolean whatsappHabilitado;
    private String whatsappDispositivo;
    @JsonIgnore
    private String whatsappApiKey;
    private String whatsappTelefonoPrueba;
    private boolean smtpTieneContrasena;
    private boolean whatsappTieneApiKey;
}
