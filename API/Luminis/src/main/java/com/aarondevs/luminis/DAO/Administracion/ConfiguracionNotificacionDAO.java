package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.ConfiguracionNotificacion;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConfiguracionNotificacionDAO implements ConfiguracionNotificacionDataSet {
    private final JdbcTemplate jdbc;

    public ConfiguracionNotificacionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ConfiguracionNotificacion obtener() {
        return jdbc.query("SELECT * FROM ConfiguracionNotificacion ORDER BY configuracionNotificacionId LIMIT 1",
                new BeanPropertyRowMapper<>(ConfiguracionNotificacion.class)).stream().findFirst().orElseGet(ConfiguracionNotificacion::new);
    }

    @Override
    public void guardarSmtp(boolean habilitado, String host, String puerto, String seguridad, String usuario,
                            String contrasenaCifrada, String remitente, String nombreRemitente, String actor) {
        jdbc.update("UPDATE ConfiguracionNotificacion SET smtpHabilitado=?, smtpHost=?, smtpPuerto=?, smtpSeguridad=?, smtpUsuario=?, "
                        + "smtpContrasena=COALESCE(?, smtpContrasena), smtpRemitente=?, smtpNombreRemitente=?, fechaModificacion=NOW(3), usuarioModificacionUuid=?",
                habilitado, host, puerto, seguridad, usuario, contrasenaCifrada, remitente, nombreRemitente, actor);
    }

    @Override
    public void guardarWhatsapp(boolean habilitado, String dispositivo, String apiKeyCifrada, String telefonoPrueba, String actor) {
        jdbc.update("UPDATE ConfiguracionNotificacion SET whatsappHabilitado=?, whatsappDispositivo=?, "
                        + "whatsappApiKey=COALESCE(?, whatsappApiKey), whatsappTelefonoPrueba=?, fechaModificacion=NOW(3), usuarioModificacionUuid=?",
                habilitado, dispositivo, apiKeyCifrada, telefonoPrueba, actor);
    }
}
