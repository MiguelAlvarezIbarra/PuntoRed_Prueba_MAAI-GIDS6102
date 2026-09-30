package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;

public interface UsuarioService {

    // Se llama internamente desde ClienteServiceImpl al registrar un cliente nuevo.
    // rol: 1 = administrador, 2 = usuario normal (usar Roles.ADMIN / Roles.USUARIO_NORMAL).
    void crearUsuarioParaCliente(Cliente cliente, String passwordPlano, Integer rol);

    LoginResponse login(LoginRequest request);

    GenericResponse logout(String tokenSinBearer);

    // Refresca ultima_actividad; la usa un filtro/interceptor en cada request autenticado.
    void registrarActividad(Integer clienteId);
}
