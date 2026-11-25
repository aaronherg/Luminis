package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.util.List;
import java.util.Map;

public interface ApiKeyAdministracionDataSet {
    List<Map<String, Object>> listar(Long usuarioAdministracionId);

    Map<String, Object> porUuid(String uuid, Long usuarioAdministracionId);

    void crear(String uuid, Long usuarioAdministracionId, String nombre, String prefijo, String hash, String actor);

    int renombrar(String uuid, Long usuarioAdministracionId, String nombre, String actor);

    int cambiarEstado(String uuid, Long usuarioAdministracionId, String estado, String actor);

    ContextoAutenticacion resolver(String hash);

    void registrarUso(String hash);
}
