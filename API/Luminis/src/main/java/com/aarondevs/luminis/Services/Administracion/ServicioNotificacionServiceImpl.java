package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Componentes.Correo.CorreoService;
import com.aarondevs.luminis.Componentes.Whatsapp.WhatsappService;
import com.aarondevs.luminis.DAO.Administracion.ConfiguracionNotificacionDataSet;
import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;
import com.aarondevs.luminis.Parameters.Administracion.ServicioSmtpParameters;
import com.aarondevs.luminis.Parameters.Administracion.ServicioWhatsappParameters;
import com.aarondevs.luminis.Componentes.Seguridad.SecretoCifrador;
import org.springframework.stereotype.Service;

@Service
public class ServicioNotificacionServiceImpl implements ServicioNotificacionService {
    private final ConfiguracionNotificacionDataSet datos;
    private final SecretoCifrador cifrador;
    private final CorreoService correo;
    private final WhatsappService whatsapp;

    public ServicioNotificacionServiceImpl(ConfiguracionNotificacionDataSet datos, SecretoCifrador cifrador,
                                           CorreoService correo, WhatsappService whatsapp) {
        this.datos = datos;
        this.cifrador = cifrador;
        this.correo = correo;
        this.whatsapp = whatsapp;
    }

    @Override
    public ConfiguracionNotificacion obtener() {
        return limpiar(datos.obtener());
    }

    @Override
    public ConfiguracionNotificacion guardarSmtp(ServicioSmtpParameters parametro, String actor) {
        if (parametro == null) throw new IllegalArgumentException("Datos requeridos");
        boolean habilitado = Boolean.TRUE.equals(parametro.getHabilitado());
        if (habilitado && (vacio(parametro.getHost()) || vacio(parametro.getUsuario()))) {
            throw new IllegalArgumentException("El servidor y el usuario SMTP son requeridos");
        }
        String contrasena = vacio(parametro.getContrasena()) ? null : cifrar(parametro.getContrasena());
        datos.guardarSmtp(habilitado, limpio(parametro.getHost()), limpio(parametro.getPuerto()),
                limpio(parametro.getSeguridad()), limpio(parametro.getUsuario()), contrasena,
                limpio(parametro.getRemitente()), limpio(parametro.getNombreRemitente()), actor);
        return obtener();
    }

    @Override
    public void probarSmtp(String destino) {
        correo.probar(destino);
    }

    @Override
    public ConfiguracionNotificacion guardarWhatsapp(ServicioWhatsappParameters parametro, String actor) {
        if (parametro == null) throw new IllegalArgumentException("Datos requeridos");
        boolean habilitado = Boolean.TRUE.equals(parametro.getHabilitado());
        if (habilitado && vacio(parametro.getDispositivo())) {
            throw new IllegalArgumentException("El dispositivo de WhatsApp es requerido");
        }
        String apiKey = vacio(parametro.getApiKey()) ? null : cifrar(parametro.getApiKey());
        datos.guardarWhatsapp(habilitado, limpio(parametro.getDispositivo()), apiKey, limpio(parametro.getTelefonoPrueba()), actor);
        return obtener();
    }

    @Override
    public void probarWhatsapp(String destino) {
        whatsapp.probar(destino);
    }

    private ConfiguracionNotificacion limpiar(ConfiguracionNotificacion c) {
        c.setSmtpTieneContrasena(!vacio(c.getSmtpContrasena()));
        c.setWhatsappTieneApiKey(!vacio(c.getWhatsappApiKey()));
        c.setSmtpContrasena(null);
        c.setWhatsappApiKey(null);
        return c;
    }

    private String cifrar(String valor) {
        try {
            return cifrador.cifrar(valor);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la credencial de forma segura");
        }
    }

    private String limpio(String v) {
        return v == null ? null : v.trim();
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}
