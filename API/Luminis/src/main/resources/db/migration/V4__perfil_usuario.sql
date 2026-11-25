ALTER TABLE UsuariosClientes
    ADD COLUMN documento TEXT NULL,
    ADD COLUMN direccion TEXT NULL,
    ADD COLUMN whatsapp TEXT NULL;

ALTER TABLE UsuariosAdministracion
    ADD COLUMN documento TEXT NULL,
    ADD COLUMN direccion TEXT NULL,
    ADD COLUMN whatsapp TEXT NULL;
