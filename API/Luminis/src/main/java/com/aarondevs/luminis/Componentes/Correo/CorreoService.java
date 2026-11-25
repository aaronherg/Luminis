package com.aarondevs.luminis.Componentes.Correo;

import com.aarondevs.luminis.DAO.Administracion.ConfiguracionNotificacionDataSet;
import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;
import com.aarondevs.luminis.Componentes.Seguridad.SecretoCifrador;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ConfiguracionNotificacionDataSet datos;
    private final SecretoCifrador cifrador;
    private final String marca;
    private final String backHost;
    private final String backPuerto;
    private final String backSeguridad;
    private final String backUsuario;
    private final String backContrasena;
    private final String backRemitente;
    private final String backNombre;

    public CorreoService(ConfiguracionNotificacionDataSet datos, SecretoCifrador cifrador,
                         @Value("${app.marca:Luminis}") String marca,
                         @Value("${SMTP_RESPALDO_HOST:}") String backHost,
                         @Value("${SMTP_RESPALDO_PUERTO:587}") String backPuerto,
                         @Value("${SMTP_RESPALDO_SEGURIDAD:STARTTLS}") String backSeguridad,
                         @Value("${SMTP_RESPALDO_USUARIO:}") String backUsuario,
                         @Value("${SMTP_RESPALDO_CONTRASENA:}") String backContrasena,
                         @Value("${SMTP_RESPALDO_REMITENTE:}") String backRemitente,
                         @Value("${SMTP_RESPALDO_NOMBRE:}") String backNombre) {
        this.datos = datos;
        this.cifrador = cifrador;
        this.marca = marca;
        this.backHost = backHost == null ? "" : backHost.trim();
        this.backPuerto = backPuerto;
        this.backSeguridad = backSeguridad;
        this.backUsuario = backUsuario == null ? "" : backUsuario.trim();
        this.backContrasena = backContrasena == null ? "" : backContrasena;
        this.backRemitente = backRemitente == null ? "" : backRemitente.trim();
        this.backNombre = backNombre == null ? "" : backNombre.trim();
    }

    public boolean configurado() {
        ConfiguracionNotificacion c = datos.obtener();
        if (Boolean.TRUE.equals(c.getSmtpHabilitado()) && !vacio(c.getSmtpHost()) && !vacio(c.getSmtpUsuario())) return true;
        return !vacio(backHost) && !vacio(backUsuario);
    }

    public boolean enviar(String para, String asunto, String html) {
        if (vacio(para)) return false;
        try {
            enviarOFallar(para, asunto, html);
            return true;
        } catch (Exception error) {
            log.warn("No se pudo enviar el correo '{}': {}", asunto, error.getMessage());
            return false;
        }
    }

    public void probar(String destino) {
        String[] cfg = configuracionEfectiva();
        enviarOFallar(vacio(destino) ? cfg[5] : destino.trim(),
                "Prueba del servicio SMTP de " + marca,
                plantilla("Prueba de correo", "La configuracion SMTP de " + marca + " puede enviar correos correctamente."));
    }

    private void enviarOFallar(String para, String asunto, String html) {
        if (vacio(para)) throw new IllegalStateException("Indica el correo de destino");
        String[] cfg = configuracionEfectiva();
        String host = cfg[0], puerto = cfg[1], seguridad = cfg[2], usuario = cfg[3], contrasena = cfg[4], remitente = cfg[5], nombre = cfg[6];
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(Integer.parseInt(vacio(puerto) ? "587" : puerto.trim()));
        sender.setUsername(usuario);
        sender.setPassword(contrasena);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "12000");
        props.put("mail.smtp.timeout", "12000");
        props.put("mail.smtp.writetimeout", "12000");
        String seg = seguridad == null ? "" : seguridad.trim().toUpperCase();
        if (seg.equals("SSL")) {
            props.put("mail.smtp.ssl.enable", "true");
        } else if (seg.equals("NONE") || seg.isBlank()) {
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            String desde = vacio(remitente) ? usuario : remitente;
            try {
                helper.setFrom(new InternetAddress(desde, vacio(nombre) ? marca : nombre));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(desde);
            }
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(html, true);
            sender.send(mensaje);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo enviar el correo: " + error.getMessage());
        }
    }

    private String[] configuracionEfectiva() {
        ConfiguracionNotificacion c = datos.obtener();
        if (Boolean.TRUE.equals(c.getSmtpHabilitado()) && !vacio(c.getSmtpHost()) && !vacio(c.getSmtpUsuario())) {
            String contrasena;
            try {
                contrasena = vacio(c.getSmtpContrasena()) ? "" : cifrador.descifrar(c.getSmtpContrasena());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer la contrasena SMTP; vuelve a guardarla");
            }
            return new String[]{c.getSmtpHost(), c.getSmtpPuerto(), c.getSmtpSeguridad(), c.getSmtpUsuario(), contrasena,
                    vacio(c.getSmtpRemitente()) ? c.getSmtpUsuario() : c.getSmtpRemitente(), c.getSmtpNombreRemitente()};
        }
        if (!vacio(backHost) && !vacio(backUsuario)) {
            return new String[]{backHost, backPuerto, backSeguridad, backUsuario, backContrasena,
                    vacio(backRemitente) ? backUsuario : backRemitente, backNombre};
        }
        throw new IllegalStateException("No hay un servicio SMTP configurado");
    }

    public String plantillaCodigo(String titulo, String intro, String codigo, String nota) {
        String cuerpo = "<p style=\"margin:0 0 16px;color:#334155;font-size:15px\">" + intro + "</p>"
                + "<div style=\"margin:0 0 16px;text-align:center\"><span style=\"display:inline-block;padding:14px 26px;border-radius:14px;background:#0b1220;color:#38bdf8;font-size:30px;font-weight:800;letter-spacing:.35em;font-family:monospace\">" + codigo + "</span></div>"
                + (nota == null ? "" : "<p style=\"margin:0;color:#64748b;font-size:13px\">" + nota + "</p>");
        return plantillaHtml(titulo, cuerpo);
    }

    public String plantilla(String titulo, String mensaje) {
        return plantillaHtml(titulo, "<p style=\"margin:0;color:#334155;font-size:15px\">" + mensaje + "</p>");
    }

    private String plantillaHtml(String titulo, String cuerpo) {
        return "<div style=\"margin:0;padding:28px;background:#0b1220;font-family:Segoe UI,Arial,sans-serif\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:18px;overflow:hidden;border:1px solid #e2e8f0\">"
                + "<div style=\"padding:20px 28px;background:linear-gradient(135deg,#0ea5e9,#2563eb);color:#fff\"><strong style=\"font-size:18px\">" + marca + "</strong></div>"
                + "<div style=\"padding:28px\"><h2 style=\"margin:0 0 14px;color:#0f172a;font-size:19px\">" + titulo + "</h2>" + cuerpo + "</div>"
                + "<div style=\"padding:16px 28px;background:#f8fafc;color:#94a3b8;font-size:12px\">Mensaje automatico de " + marca + ". Si no reconoces esta actividad, cambia tu contrasena.</div>"
                + "</div></div>";
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}
