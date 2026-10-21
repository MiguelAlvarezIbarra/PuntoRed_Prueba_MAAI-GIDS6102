package com.proyecto.servicios.service;

import com.proyecto.servicios.model.*;

/**
 * Servicio de negocio para la administracion de clientes.
 * Separa responsabilidades entre capa de presentacion (Controller) y persistencia (Repository).
 */
public interface ClienteService {

    ClienteResponse creaCliente(ClienteRequest request);

    ClienteResponse creaClienteConRol(ClienteRequest request, Integer rol);

    ClienteResponse actualizaCliente(ClienteActualizaRequest request);

    GenericResponse eliminaCliente(ClienteBajaRequest request);

    GenericResponse reactivarCliente(ClienteReactivarRequest request);

    ListaClientesResponse buscarDinamico(ClienteSearchRequest request, String authorizationHeader);
}
