package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.DAO.Clientes.ClienteDataSet;
import com.aarondevs.luminis.Models.Clientes.Cliente;
import com.aarondevs.luminis.Parameters.Clientes.ClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteDatosParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {
    private static final Set<String> TIPOS = Set.of("Empresa", "Persona");

    private final ClienteDataSet datos;
    private final UsuarioClienteService usuarioClienteService;
    private final String rolPropietario;

    public ClienteServiceImpl(ClienteDataSet datos, UsuarioClienteService usuarioClienteService,
                          @Value("${app.luminis.cliente.rol-propietario}") String rolPropietario) {
        this.datos = datos;
        this.usuarioClienteService = usuarioClienteService;
        this.rolPropietario = rolPropietario == null || rolPropietario.isBlank() ? "Propietario" : rolPropietario.trim();
    }

    @Override
    @Transactional
    public Cliente crear(ClienteAgregarParameters parametro, String actor) {
        validar(parametro, null);
        if (parametro.getNombres() == null || parametro.getNombres().isBlank()) {
            throw new IllegalArgumentException("nombres del usuario propietario es requerido");
        }
        Cliente cliente = datos.create(parametro, actor);
        datos.crearSuscripcion(cliente.getClienteId(), actor);
        Long rolId = datos.crearRolPropietario(cliente.getClienteId(), rolPropietario, actor);

        UsuarioClienteAgregarParameters propietario = new UsuarioClienteAgregarParameters();
        propietario.setClienteId(cliente.getClienteId());
        propietario.setUsuario(parametro.getUsuario());
        propietario.setContrasena(parametro.getContrasena());
        propietario.setNombres(parametro.getNombres());
        propietario.setApellidos(parametro.getApellidos());
        propietario.setCorreo(parametro.getCorreo());
        propietario.setPrefijoTelefono(parametro.getPrefijoTelefono());
        propietario.setTelefono(parametro.getTelefono());
        propietario.setRolesIds(List.of(rolId));
        usuarioClienteService.crear(propietario, true, actor);
        return datos.findById(cliente.getClienteId());
    }

    @Override
    public Cliente actualizar(ClienteActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getClienteUuid() == null || parametro.getClienteUuid().isBlank()) {
            throw new IllegalArgumentException("clienteUuid es requerido");
        }
        validar(parametro, parametro.getClienteUuid());
        if (parametro.getEstado() != null && !Set.of("Activo", "Suspendido").contains(parametro.getEstado())) {
            throw new IllegalArgumentException("estado debe ser Activo o Suspendido");
        }
        return datos.update(parametro, actor);
    }

    @Override
    public Cliente id(Long id) {
        return datos.findById(id);
    }

    @Override
    public Cliente uuid(String uuid) {
        return datos.findByUuid(uuid);
    }

    @Override
    public Object all() {
        return datos.findAll();
    }

    @Override
    public Object pages(Integer page, Integer size, String buscar, String estado) {
        return datos.findAllPages(page, size, buscar, estado);
    }

    @Override
    public boolean eliminar(Long id, String actor) {
        return datos.delete(id, actor);
    }

    private void validar(ClienteDatosParameters parametro, String uuid) {
        if (parametro == null || parametro.getNombreComercial() == null || parametro.getNombreComercial().isBlank()
                || parametro.getCorreo() == null || !parametro.getCorreo().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("nombreComercial y un correo válido son requeridos");
        }
        if (parametro.getTipoCliente() == null || !TIPOS.contains(parametro.getTipoCliente())) {
            throw new IllegalArgumentException("tipoCliente debe ser Empresa o Persona");
        }
        if (parametro.getDiaCorte() != null && (parametro.getDiaCorte() < 1 || parametro.getDiaCorte() > 28)) {
            throw new IllegalArgumentException("diaCorte debe estar entre 1 y 28");
        }
        if (datos.existsDocumento(parametro.getDocumento(), uuid)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese documento");
        }
    }
}
