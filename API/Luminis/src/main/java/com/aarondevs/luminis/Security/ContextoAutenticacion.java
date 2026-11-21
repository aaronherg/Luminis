package com.aarondevs.luminis.Security;

public record ContextoAutenticacion(
        Long usuarioId, String usuarioUuid, String tipo, String tokenJti,
        Long clienteId, String clienteUuid,
        Long usuarioClienteId, String usuarioClienteUuid,
        Long usuarioAdministracionId, String usuarioAdministracionUuid
) {
    public static final String ADMINISTRACION = "Administracion";
    public static final String CLIENTE = "Cliente";

    public boolean esAdministracion() {
        return ADMINISTRACION.equalsIgnoreCase(tipo);
    }

    public boolean esCliente() {
        return CLIENTE.equalsIgnoreCase(tipo);
    }
}
