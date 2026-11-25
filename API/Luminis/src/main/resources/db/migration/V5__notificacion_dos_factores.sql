ALTER TABLE Usuarios ADD COLUMN dosFactores BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE CodigosVerificacion (
    codigoVerificacionId BIGINT NOT NULL AUTO_INCREMENT,
    usuarioId BIGINT NOT NULL,
    proposito VARCHAR(40) NOT NULL,
    codigoHash TEXT NOT NULL,
    expira DATETIME(3) NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    intentos INT NOT NULL DEFAULT 0,
    fechaCreacion DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (codigoVerificacionId),
    KEY ix_codigos_usuario (usuarioId, proposito, usado),
    CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuarioId) REFERENCES Usuarios(usuarioId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ConfiguracionNotificacion (
    configuracionNotificacionId BIGINT NOT NULL AUTO_INCREMENT,
    smtpHabilitado BOOLEAN NOT NULL DEFAULT FALSE,
    smtpHost TEXT NULL,
    smtpPuerto TEXT NULL,
    smtpSeguridad TEXT NULL,
    smtpUsuario TEXT NULL,
    smtpContrasena TEXT NULL,
    smtpRemitente TEXT NULL,
    smtpNombreRemitente TEXT NULL,
    whatsappHabilitado BOOLEAN NOT NULL DEFAULT FALSE,
    whatsappDispositivo TEXT NULL,
    whatsappApiKey TEXT NULL,
    whatsappTelefonoPrueba TEXT NULL,
    fechaModificacion DATETIME(3) NULL,
    usuarioModificacionUuid TEXT NULL,
    PRIMARY KEY (configuracionNotificacionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ConfiguracionNotificacion (smtpHabilitado, whatsappHabilitado) VALUES (FALSE, FALSE);
