package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.ClienteData;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.DomicilioRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaClientesResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.UsuarioService;
import com.proyecto.servicios.util.GeneradorCuenta;
import com.proyecto.servicios.util.Roles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DomicilioRepository domicilioRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private GeneradorCuenta generadorCuenta;

    @Value("${cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicial;

    private static final int EDAD_MINIMA = 18;
    private static final int INTENTOS_MAX_NUMERO_CUENTA = 20;

    @Override
    @Transactional
    public ClienteResponse creaCliente(ClienteRequest request) {
        // Registro publico: SIEMPRE rol de usuario normal, sin excepcion.
        return creaClienteInterno(request, Roles.USUARIO_NORMAL);
    }

    @Override
    @Transactional
    public ClienteResponse creaClienteConRol(ClienteRequest request, Integer rol) {
        // Usado solo por el endpoint de administrador (ya protegido con AdminGuard).
        if (rol == null || (rol != Roles.ADMIN && rol != Roles.USUARIO_NORMAL)) {
            throw new ValidacionException("El rol debe ser 1 (administrador) o 2 (usuario normal)");
        }
        return creaClienteInterno(request, rol);
    }

    private ClienteResponse creaClienteInterno(ClienteRequest request, Integer rol) {
        log.info("Inicio creaCliente - curp={} rol={}", request.getCurp(), rol);

        validarMayorEdad(request.getFechaNacimiento());
        validarPasswordPresente(request.getPassword());
        validarUnicidad(request);
        validarCatalogos(request);

        Cliente cliente = new Cliente();
        copiarDatosBasicos(cliente, request);
        clienteRepository.save(cliente);

        Domicilio domicilio = mapearDomicilio(request.getDomicilio(), cliente);
        domicilioRepository.save(domicilio);

        Cuenta cuenta = crearCuentaParaCliente(cliente);

        usuarioService.crearUsuarioParaCliente(cliente, request.getPassword(), rol);

        ClienteResponse response = construirRespuesta(cliente, domicilio, cuenta, 0, "Cliente registrado correctamente");
        log.info("Fin creaCliente - clienteId={} numeroCuenta={}", cliente.getId(), cuenta.getNumeroCuenta());
        return response;
    }

    @Override
    @Transactional
    public ClienteResponse actualizaCliente(Integer id, ClienteRequest request) {
        log.info("Inicio actualizaCliente - id={}", id);
        validarCatalogos(request);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("El cliente con id " + id + " no existe"));

        // CURP, RFC y numero de cuenta NUNCA se modifican, aunque vengan en el request.
        if (request.getCorreo() != null && !request.getCorreo().equalsIgnoreCase(cliente.getCorreo())) {
            if (clienteRepository.existsByCorreo(request.getCorreo())) {
                throw new CorreoDuplicadoException("Ya existe un cliente registrado con ese correo");
            }
            cliente.setCorreo(request.getCorreo());
        }

        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        clienteRepository.save(cliente);

        Domicilio domicilio = domicilioRepository.findByCliente_Id(cliente.getId())
                .orElseGet(() -> {
                    Domicilio nuevo = new Domicilio();
                    nuevo.setCliente(cliente);
                    return nuevo;
                });
        aplicarDomicilio(domicilio, request.getDomicilio());
        domicilioRepository.save(domicilio);

        Cuenta cuenta = cuentaRepository.findByCliente_Id(cliente.getId()).stream().findFirst().orElse(null);

        ClienteResponse response = construirRespuesta(cliente, domicilio, cuenta, 0, "Cliente actualizado correctamente");
        log.info("Fin actualizaCliente - id={}", id);
        return response;
    }

    @Override
    @Transactional
    public GenericResponse eliminaCliente(Integer id) {
        log.info("Inicio eliminaCliente (baja logica) - id={}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("El cliente con id " + id + " no existe"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Regla de negocio: solo clientes activos pueden tener cuentas activas.
        List<Cuenta> cuentas = cuentaRepository.findByCliente_Id(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setEstatus("INACTIVA");
            cuentaRepository.save(cuenta);
        }

        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Cliente dado de baja correctamente");
        log.info("Fin eliminaCliente - id={}", id);
        return response;
    }

    @Override
    public ListaClientesResponse obtenerClientes() {
        List<ClienteData> data = clienteRepository.findAll().stream()
                .map(this::toClienteData)
                .collect(Collectors.toList());
        return construirLista(data);
    }

    @Override
    public ClienteResponse obtenerClientePorId(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("El cliente con id " + id + " no existe"));
        return construirRespuestaDesdeCliente(cliente);
    }

    @Override
    public ClienteResponse obtenerClientePorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con esa CURP"));
        return construirRespuestaDesdeCliente(cliente);
    }

    @Override
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con ese RFC"));
        return construirRespuestaDesdeCliente(cliente);
    }

    @Override
    public ClienteResponse obtenerClientePorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe un cliente con ese correo"));
        return construirRespuestaDesdeCliente(cliente);
    }

    @Override
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("No existe una cuenta con ese numero"));
        return construirRespuestaDesdeCliente(cuenta.getCliente());
    }

    @Override
    public ListaClientesResponse obtenerClientesActivos() {
        List<ClienteData> data = clienteRepository.findByActivoTrue().stream()
                .map(this::toClienteData)
                .collect(Collectors.toList());
        return construirLista(data);
    }

    @Override
    public ListaClientesResponse obtenerClientesPorRangoFechas(LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);
        List<ClienteData> data = clienteRepository.findByFechaRegistroBetween(desdeDT, hastaDT).stream()
                .map(this::toClienteData)
                .collect(Collectors.toList());
        return construirLista(data);
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    private void validarCatalogos(ClienteRequest request) {
        if (clienteRepository.countNacionalidad(request.getNacionalidad()) == 0) {
            throw new ValidacionException("Código 7: La nacionalidad proporcionada no es válida o no existe en el catálogo");
        }
        if (request.getDomicilio() != null && request.getDomicilio().getPais() != null) {
            if (clienteRepository.countPais(request.getDomicilio().getPais()) == 0) {
                throw new ValidacionException("Código 7: El país proporcionado no es válido o no existe en el catálogo");
            }
        }
    }

    private void validarMayorEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ValidacionException("El cliente debe ser mayor de edad (18 anios o mas)");
        }
    }

    private void validarPasswordPresente(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidacionException("El password es obligatorio para registrar el cliente");
        }
    }

    private void validarUnicidad(ClienteRequest request) {
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException("Ya existe un cliente registrado con esa CURP");
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException("Ya existe un cliente registrado con ese RFC");
        }
        if (clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoDuplicadoException("Ya existe un cliente registrado con ese correo");
        }
    }

    private void copiarDatosBasicos(Cliente cliente, ClienteRequest request) {
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreo(request.getCorreo());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);
    }

    private Domicilio mapearDomicilio(DomicilioRequest req, Cliente cliente) {
        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        aplicarDomicilio(domicilio, req);
        return domicilio;
    }

    private void aplicarDomicilio(Domicilio domicilio, DomicilioRequest req) {
        domicilio.setCalle(req.getCalle());
        domicilio.setNumeroExterior(req.getNumeroExterior());
        domicilio.setNumeroInterior(req.getNumeroInterior());
        domicilio.setColonia(req.getColonia());
        domicilio.setMunicipio(req.getMunicipio());
        domicilio.setEstado(req.getEstado());
        domicilio.setCodigoPostal(req.getCodigoPostal());
        domicilio.setPais(req.getPais());
    }

    private Cuenta crearCuentaParaCliente(Cliente cliente) {
        String numero = null;
        for (int intento = 0; intento < INTENTOS_MAX_NUMERO_CUENTA; intento++) {
            String candidato = generadorCuenta.generarCandidato();
            if (!cuentaRepository.existsByNumeroCuenta(candidato)) {
                numero = candidato;
                break;
            }
        }
        if (numero == null) {
            throw new ValidacionException("No fue posible generar un numero de cuenta unico, intenta de nuevo");
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(numero);
        cuenta.setSaldo(saldoInicial.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : saldoInicial);
        cuenta.setEstatus("ACTIVA");
        cuentaRepository.save(cuenta);
        return cuenta;
    }

    private ClienteResponse construirRespuestaDesdeCliente(Cliente cliente) {
        Domicilio domicilio = domicilioRepository.findByCliente_Id(cliente.getId()).orElse(null);
        Cuenta cuenta = cuentaRepository.findByCliente_Id(cliente.getId()).stream().findFirst().orElse(null);
        return construirRespuesta(cliente, domicilio, cuenta, 0, "Exito");
    }

    private ClienteResponse construirRespuesta(Cliente cliente, Domicilio domicilio, Cuenta cuenta, Integer codigo, String mensaje) {
        ClienteResponse response = new ClienteResponse();
        response.setCliente(toClienteData(cliente, domicilio, cuenta));
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return response;
    }

    private ListaClientesResponse construirLista(List<ClienteData> data) {
        ListaClientesResponse response = new ListaClientesResponse();
        response.setClientes(data);
        response.setCodigo(0);
        response.setMensaje("Exito");
        return response;
    }

    private ClienteData toClienteData(Cliente cliente) {
        Domicilio domicilio = domicilioRepository.findByCliente_Id(cliente.getId()).orElse(null);
        Cuenta cuenta = cuentaRepository.findByCliente_Id(cliente.getId()).stream().findFirst().orElse(null);
        return toClienteData(cliente, domicilio, cuenta);
    }

    private ClienteData toClienteData(Cliente cliente, Domicilio domicilio, Cuenta cuenta) {
        ClienteData data = new ClienteData();
        data.setId(cliente.getId());
        data.setNombre(cliente.getNombre());
        data.setSegundoNombre(cliente.getSegundoNombre());
        data.setApellidoPaterno(cliente.getApellidoPaterno());
        data.setApellidoMaterno(cliente.getApellidoMaterno());
        data.setFechaNacimiento(cliente.getFechaNacimiento());
        data.setCurp(cliente.getCurp());
        data.setRfc(cliente.getRfc());
        data.setSexo(cliente.getSexo());
        data.setNacionalidad(cliente.getNacionalidad());
        data.setEstadoCivil(cliente.getEstadoCivil());
        data.setCorreo(cliente.getCorreo());
        data.setTelefonoMovil(cliente.getTelefonoMovil());
        data.setTelefonoAlternativo(cliente.getTelefonoAlternativo());
        data.setOcupacion(cliente.getOcupacion());
        data.setEmpresa(cliente.getEmpresa());
        data.setIngresoMensual(cliente.getIngresoMensual());
        data.setActivo(cliente.getActivo());
        data.setFechaRegistro(cliente.getFechaRegistro());

        if (domicilio != null) {
            DomicilioRequest domicilioDto = new DomicilioRequest();
            domicilioDto.setCalle(domicilio.getCalle());
            domicilioDto.setNumeroExterior(domicilio.getNumeroExterior());
            domicilioDto.setNumeroInterior(domicilio.getNumeroInterior());
            domicilioDto.setColonia(domicilio.getColonia());
            domicilioDto.setMunicipio(domicilio.getMunicipio());
            domicilioDto.setEstado(domicilio.getEstado());
            domicilioDto.setCodigoPostal(domicilio.getCodigoPostal());
            domicilioDto.setPais(domicilio.getPais());
            data.setDomicilio(domicilioDto);
        }

        if (cuenta != null) {
            data.setNumeroCuenta(cuenta.getNumeroCuenta());
        }

        return data;
    }
}
