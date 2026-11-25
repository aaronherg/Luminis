package com.aarondevs.luminis.Componentes.Whatsapp;

import com.aarondevs.luminis.DAO.Administracion.ConfiguracionNotificacionDataSet;
import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;
import com.aarondevs.luminis.Componentes.Seguridad.SecretoCifrador;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsappService {
    private final ConfiguracionNotificacionDataSet datos;
    private final SecretoCifrador cifrador;
    private final String url;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsappService(ConfiguracionNotificacionDataSet datos, SecretoCifrador cifrador,
                           @Value("${MENSAJEO_WHATSAPP_URL:}") String url) {
        this.datos = datos;
        this.cifrador = cifrador;
        this.url = url == null ? "" : url.trim();
    }

    public boolean configurado() {
        ConfiguracionNotificacion c = datos.obtener();
        return Boolean.TRUE.equals(c.getWhatsappHabilitado()) && !vacio(c.getWhatsappDispositivo()) && !vacio(c.getWhatsappApiKey());
    }

    public void probar(String destino) {
        ConfiguracionNotificacion c = datos.obtener();
        if (!Boolean.TRUE.equals(c.getWhatsappHabilitado())) throw new IllegalStateException("El servicio de WhatsApp esta desactivado");
        String telefono = vacio(destino) ? c.getWhatsappTelefonoPrueba() : destino.trim();
        if (vacio(telefono)) throw new IllegalStateException("Indica el telefono de destino de la prueba");
        if (vacio(c.getWhatsappDispositivo()) || vacio(c.getWhatsappApiKey())) throw new IllegalStateException("Configura el dispositivo y la API key de WhatsApp");
        if (vacio(url)) throw new IllegalStateException("No hay un servicio de WhatsApp disponible");
        String apiKey;
        try {
            apiKey = cifrador.descifrar(c.getWhatsappApiKey());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la API key; vuelve a guardarla");
        }
        try {
            String cuerpo = "{\"dispositivo\":\"" + c.getWhatsappDispositivo() + "\",\"apiKey\":\"" + apiKey + "\",\"telefono\":\"" + telefono + "\",\"mensaje\":\"Prueba de WhatsApp de Luminis\"}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) throw new IllegalStateException("El servicio respondio " + resp.statusCode());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar la prueba: " + e.getMessage());
        }
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}
