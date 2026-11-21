package com.aarondevs.luminis.DAO.Mensajeria;

import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import java.util.List;

public interface ApiKeyDataSet {
    List<ApiKey> findAll(Long clienteId);

    ApiKey findById(Long apiKeyId);

    ApiKey findActivaByHash(String hash);

    Long create(Long clienteId, String nombre, String prefijo, String hash, String permisos, String actor);

    int update(Long apiKeyId, String nombre, String permisos, String estado, String actor);

    int revocar(Long apiKeyId, String actor);

    void registrarUso(Long apiKeyId);

    long countActivas(Long clienteId);
}
