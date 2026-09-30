package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaClientesResponse;

import java.time.LocalDate;

public interface ClienteService {

    ClienteResponse creaCliente(ClienteRequest request);

    // Usado solo por el endpoint de administrador: permite elegir el rol (1 admin, 2 normal)
    ClienteResponse creaClienteConRol(ClienteRequest request, Integer rol);

    ClienteResponse actualizaCliente(Integer id, ClienteRequest request);

    GenericResponse eliminaCliente(Integer id);

    ListaClientesResponse obtenerClientes();

    ClienteResponse obtenerClientePorId(Integer id);

    ClienteResponse obtenerClientePorCurp(String curp);

    ClienteResponse obtenerClientePorRfc(String rfc);

    ClienteResponse obtenerClientePorCorreo(String correo);

    ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta);

    ListaClientesResponse obtenerClientesActivos();

    ListaClientesResponse obtenerClientesPorRangoFechas(LocalDate desde, LocalDate hasta);
}
