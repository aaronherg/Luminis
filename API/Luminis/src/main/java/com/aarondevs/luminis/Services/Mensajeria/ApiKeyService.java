package com.aarondevs.luminis.Services.Mensajeria;

import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyActualizarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyAgregarParameters;
import java.util.List;

public interface ApiKeyService {
    List<String> PERMISOS = List.of("conectar", "enviar", "publicar", "consultar_presencia", "desconectar", "consultar_cuenta");

    ApiKey crear(ApiKeyAgregarParameters parametro, Long clienteId, String actor);

    ApiKey actualizar(ApiKeyActualizarParameters parametro, String actor);

    boolean revocar(Long apiKeyId, String actor);

    ApiKey id(Long apiKeyId);

    List<ApiKey> all(Long clienteId);

    ApiKey autenticar(String clave, String permiso);
}
