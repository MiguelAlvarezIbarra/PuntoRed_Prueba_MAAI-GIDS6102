package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.config.ActividadSesionInterceptor;
import com.proyecto.servicios.config.AdminGuard;
import com.proyecto.servicios.config.ConfigDB;
import com.proyecto.servicios.config.OpenApi;
import com.proyecto.servicios.config.RedisConfig;
import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.config.WebConfig;
import com.proyecto.servicios.controller.AdminController;
import com.proyecto.servicios.controller.AuthController;
import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.controller.CuentaController;
import com.proyecto.servicios.controller.PagoController;
import com.proyecto.servicios.controller.ProductoController;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.AdminClienteRequest;
import com.proyecto.servicios.model.ClienteActualizaRequest;
import com.proyecto.servicios.model.ClienteBajaRequest;
import com.proyecto.servicios.model.ClienteReactivarRequest;
import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.ClienteSearchRequest;
import com.proyecto.servicios.model.CuentaAbonoRequest;
import com.proyecto.servicios.model.CuentaConsultaRequest;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.DomicilioRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaClientesResponse;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.ProductoData;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.sf.CatNacionalidadRepository;
import com.proyecto.servicios.repositorys.sf.CatPaisRepository;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.ClienteSpecification;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.DomicilioRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import com.proyecto.servicios.service.Impl.SesionExpiracionScheduler;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import com.proyecto.servicios.util.CryptoUtil;
import com.proyecto.servicios.util.GeneradorCuenta;
import com.proyecto.servicios.util.HashUtil;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import com.proyecto.servicios.util.ValidadorRfcCurp;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreServicesAndExceptionHandlerTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DomicilioRepository domicilioRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private CatPaisRepository catPaisRepository;
    @Mock
    private CatNacionalidadRepository catNacionalidadRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SeguridadGuard seguridadGuard;
    @Mock
    private UsuarioRepository usuarioRepository;

    private static final String LLAVE_AES_B64 = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String LLAVE_JWT_B64 = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlAHPo7OLqB4Fkk4E2yGOtw";

    @Test
    void utilidades_cryptoJwtHashGeneradorYValidadorRfcCurp() {
        CryptoUtil cryptoUtil = new CryptoUtil(LLAVE_AES_B64);
        String cifrado = cryptoUtil.cifrar("secreto@correo.com");
        assertThat(cryptoUtil.descifrar(cifrado)).isEqualTo("secreto@correo.com");
        assertThrows(IllegalStateException.class, () -> cryptoUtil.descifrar("invalido"));

        JwtUtil jwtUtil = new JwtUtil(LLAVE_JWT_B64, 15L);
        String jwt = jwtUtil.generar(10, "hash123", Roles.ADMIN);
        assertThat(jwtUtil.obtenerClienteId(jwt)).isEqualTo(10);
        assertThat(jwtUtil.obtenerRol(jwt)).isEqualTo(Roles.ADMIN);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtUtil.obtenerClienteId("token.malo"));

        GeneradorCuenta gen = new GeneradorCuenta();
        assertThat(gen.generarCandidato()).matches("^[1-9]\\d{17}$");

        ClienteRequest req = crearClienteRequestBase();
        ValidadorRfcCurp.validar(req);

        req.setNombre("JOSE JUAN");
        req.setRfc("GODJ900515ABC");
        req.setCurp("GODJ900515HDFR01");
        ValidadorRfcCurp.validar(req);

        req.setSexo("M");
        assertThrows(ValidacionException.class, () -> ValidadorRfcCurp.validar(req));

        req.setCurp("XXXX900515MDFR01");
        assertThrows(ValidacionException.class, () -> ValidadorRfcCurp.validar(req));

        req.setRfc("XXXX900515ABC");
        assertThrows(ValidacionException.class, () -> ValidadorRfcCurp.validar(req));

        ClienteActualizaRequest actReq = new ClienteActualizaRequest();
        actReq.setNombre("JUAN");
        actReq.setApellidoPaterno("GOMEZ");
        actReq.setApellidoMaterno("DIAZ");
        actReq.setRfc("GODJ900515ABC");
        LocalDate fechaNac = LocalDate.of(1990, 5, 15);
        ValidadorRfcCurp.validarActualizacion(actReq, fechaNac);
        ValidadorRfcCurp.validarActualizacion(actReq, null);

        actReq.setRfc("XXXX900515ABC");
        assertThrows(ValidacionException.class,
                () -> ValidadorRfcCurp.validarActualizacion(actReq, fechaNac));
    }

    @Test
    void cuentaService_operacionesCompletas() {
        CuentaServiceImpl cuentaService = new CuentaServiceImpl(cuentaRepository);

        Cliente cli = new Cliente();
        cli.setId(5);
        cli.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setCliente(cli);
        cuenta.setNumeroCuenta("123456789012345678");
        cuenta.setSaldo(new BigDecimal("100.00"));
        cuenta.setEstatus("ACTIVA");

        when(cuentaRepository.findByNumeroCuenta("123456789012345678")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(cuenta));

        assertThat(cuentaService.obtenerCuentaPorNumero("123456789012345678").getCodigo()).isZero();
        assertThat(cuentaService.obtenerCuentasActivas().getCuentas()).hasSize(1);
        assertThat(cuentaService.consultarSaldo("123456789012345678").getSaldo()).isEqualByComparingTo("100");

        CuentaAbonoRequest abono = new CuentaAbonoRequest();
        abono.setNumeroCuenta("123456789012345678");
        abono.setMonto(new BigDecimal("50.00"));
        assertThat(cuentaService.abonarSaldo(abono).getSaldo()).isEqualByComparingTo("150");

        cli.setActivo(false);
        assertThrows(ValidacionException.class, () -> cuentaService.abonarSaldo(abono));
    }

    @Test
    void usuarioServiceYSesionScheduler_operacionesCompletas() {
        CryptoUtil cryptoUtil = new CryptoUtil(LLAVE_AES_B64);
        HashUtil hashUtil = new HashUtil();
        JwtUtil jwtUtil = new JwtUtil(LLAVE_JWT_B64, 15L);
        UsuarioServiceImpl usuarioServiceImpl = new UsuarioServiceImpl(usuarioRepository, cryptoUtil, hashUtil, jwtUtil);
        ReflectionTestUtils.setField(usuarioServiceImpl, "expiracionMinutos", 15L);

        Cliente cli = new Cliente();
        cli.setId(8);
        cli.setCorreo("user@test.com");
        cli.setActivo(true);

        usuarioServiceImpl.crearUsuarioParaCliente(cli, "Password123!", Roles.USUARIO_NORMAL);
        verify(usuarioRepository).save(any(Usuario.class));

        Usuario u = new Usuario();
        u.setCliente(cli);
        u.setRol(Roles.USUARIO_NORMAL);
        u.setPasswordHash(new BCryptPasswordEncoder().encode("Password123!"));
        u.setSesionActiva(true);

        when(usuarioRepository.findByCorreoHash(anyString())).thenReturn(Optional.of(u));
        LoginRequest loginReq = new LoginRequest();
        loginReq.setCorreo("user@test.com");
        loginReq.setPassword("Password123!");

        LoginResponse loginResp = usuarioServiceImpl.login(loginReq);
        assertThat(loginResp.getCodigo()).isZero();

        // Logout valido e invalido
        when(usuarioRepository.findByCliente_Id(8)).thenReturn(Optional.of(u));
        assertThat(usuarioServiceImpl.logout(loginResp.getJwt()).getCodigo()).isZero();
        assertThat(usuarioServiceImpl.logout("jwt-invalido").getCodigo()).isEqualTo(1);

        // Registrar actividad
        u.setSesionActiva(true);
        usuarioServiceImpl.registrarActividad(8);

        // Login fallido por password o inactivo
        cli.setActivo(false);
        assertThrows(ValidacionException.class, () -> usuarioServiceImpl.login(loginReq));
        loginReq.setPassword("WrongPass");
        assertThrows(ValidacionException.class, () -> usuarioServiceImpl.login(loginReq));

        // Scheduler de sesiones
        SesionExpiracionScheduler scheduler = new SesionExpiracionScheduler(usuarioRepository);
        ReflectionTestUtils.setField(scheduler, "minutosInactividad", 3L);
        when(usuarioRepository.existsBySesionActivaTrueAndUltimaActividadBefore(any())).thenReturn(true);
        when(usuarioRepository.findBySesionActivaTrueAndUltimaActividadBefore(any())).thenReturn(List.of(u));
        scheduler.cerrarSesionesInactivas();
        assertThat(u.getSesionActiva()).isFalse();
    }

    @Test
    void clienteService_flujoCompleto() {
        ClienteServiceImpl clienteService = new ClienteServiceImpl(
                clienteRepository,
                domicilioRepository,
                cuentaRepository,
                catPaisRepository,
                catNacionalidadRepository,
                usuarioService,
                seguridadGuard
        );

        ClienteRequest req = crearClienteRequestBase();
        when(catNacionalidadRepository.existsByNombre(anyString())).thenReturn(true);
        when(catPaisRepository.existsByNombre(anyString())).thenReturn(true);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        ClienteResponse creado = clienteService.creaCliente(req);
        assertThat(creado.getCodigo()).isZero();

        assertThat(clienteService.creaClienteConRol(req, Roles.ADMIN).getCodigo()).isZero();
        assertThrows(ValidacionException.class, () -> clienteService.creaClienteConRol(req, 99));

        Cliente existente = new Cliente();
        existente.setId(15);
        existente.setNombre("JUAN");
        existente.setApellidoPaterno("GOMEZ");
        existente.setApellidoMaterno("DIAZ");
        existente.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        existente.setRfc("GODJ900515ABC");
        existente.setCurp("GODJ900515HDFR01");
        existente.setCorreo("viejo@correo.com");
        existente.setActivo(true);

        when(clienteRepository.findByRfc("GODJ900515ABC")).thenReturn(Optional.of(existente));
        when(domicilioRepository.findByCliente_Id(15)).thenReturn(Optional.empty());

        Cuenta cta = new Cuenta();
        cta.setNumeroCuenta("123456789012345678");
        cta.setEstatus("ACTIVA");
        when(cuentaRepository.findByCliente_Id(15)).thenReturn(List.of(cta));

        ClienteActualizaRequest act = new ClienteActualizaRequest();
        act.setRfc("GODJ900515ABC");
        act.setNombre("JUAN");
        act.setApellidoPaterno("GOMEZ");
        act.setApellidoMaterno("DIAZ");
        act.setSexo("H");
        act.setNacionalidad("Mexicana");
        act.setCorreo("nuevo@correo.com");
        act.setDomicilio(req.getDomicilio());

        assertThat(clienteService.actualizaCliente(act).getCodigo()).isZero();

        // Baja y reactivacion
        when(cuentaRepository.existsByCliente_IdAndEstatus(15, "ACTIVA")).thenReturn(true);
        ClienteBajaRequest baja = new ClienteBajaRequest();
        baja.setRfc("GODJ900515ABC");
        assertThat(clienteService.eliminaCliente(baja).getCodigo()).isZero();
        assertThrows(ValidacionException.class, () -> clienteService.eliminaCliente(baja));

        when(cuentaRepository.existsByCliente_IdAndEstatus(15, "INACTIVA")).thenReturn(true);
        ClienteReactivarRequest react = new ClienteReactivarRequest();
        react.setRfc("GODJ900515ABC");
        assertThat(clienteService.reactivarCliente(react).getCodigo()).isZero();
        assertThrows(ValidacionException.class, () -> clienteService.reactivarCliente(react));

        // Busqueda dinamica
        when(seguridadGuard.extraerInfoToken("Bearer ok")).thenReturn(new InfoToken(15, Roles.USUARIO_NORMAL));
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(List.of(existente));
        ClienteSearchRequest searchReq = new ClienteSearchRequest();
        searchReq.setNombre("JUAN");
        assertThat(clienteService.buscarDinamico(searchReq, "Bearer ok").getClientes()).hasSize(1);

        when(seguridadGuard.extraerInfoToken(null)).thenReturn(null);
        assertThrows(ValidacionException.class, () -> clienteService.buscarDinamico(searchReq, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void clienteSpecification_construyePredicadosDinamicos() {
        ClienteSearchRequest req = new ClienteSearchRequest();
        req.setNombre("Juan");
        req.setRfc("GODE");
        req.setCurp("GODE");
        req.setCorreo("juan@");
        req.setNumeroCuenta("1234");
        req.setFechaDesde(LocalDate.of(2026, 1, 1));
        req.setFechaHasta(LocalDate.of(2026, 12, 31));
        req.setActivo(true);

        Root<Cliente> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> pathObj = mock(Path.class);
        Expression<String> exprStr = mock(Expression.class);
        Predicate pred = mock(Predicate.class);
        Subquery<Integer> subquery = mock(Subquery.class);
        Root<Cuenta> cuentaRoot = mock(Root.class);

        lenient().when(root.get(anyString())).thenReturn(pathObj);
        lenient().when(cuentaRoot.get(anyString())).thenReturn(pathObj);
        lenient().when(pathObj.get(anyString())).thenReturn(pathObj);
        lenient().when(pathObj.in(any(Subquery.class))).thenReturn(pred);
        lenient().when(cb.concat(any(Expression.class), anyString())).thenReturn(exprStr);
        lenient().when(cb.concat(any(Expression.class), any(Expression.class))).thenReturn(exprStr);
        lenient().when(cb.coalesce(any(), anyString())).thenReturn(exprStr);
        lenient().when(cb.lower(any())).thenReturn(exprStr);
        lenient().when(cb.like(any(), anyString())).thenReturn(pred);
        lenient().when(cb.equal(any(), any())).thenReturn(pred);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(pred);
        lenient().when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(pred);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(pred);
        lenient().when(query.subquery(Integer.class)).thenReturn(subquery);
        lenient().when(subquery.from(Cuenta.class)).thenReturn(cuentaRoot);
        lenient().when(subquery.select(any())).thenReturn(subquery);
        lenient().when(subquery.where(any(Predicate.class))).thenReturn(subquery);

        Specification<Cliente> spec = ClienteSpecification.buscar(req, 10);
        assertThat(spec.toPredicate(root, query, cb)).isNotNull();

        Specification<Cliente> specVacia = ClienteSpecification.buscar(new ClienteSearchRequest(), null);
        assertThat(specVacia.toPredicate(root, query, cb)).isNotNull();
    }

    @Test
    void globalExceptionHandler_centralizaTodasLasExcepcionesEnUnSoloHandler() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // 1. ValidacionException
        ResponseEntity<GenericResponse> r1 = handler.handleException(
                new ValidacionException(MensajeError.SALDO_INSUFICIENTE));
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r1.getBody().getCodigo()).isEqualTo(15);

        ResponseEntity<GenericResponse> r1b = handler.handleException(new ValidacionException("Error libre"));
        assertThat(r1b.getBody().getCodigo()).isEqualTo(1);

        ResponseEntity<GenericResponse> r1c = handler.handleException(new ValidacionException(24, "Código 24: Rechazado"));
        assertThat(r1c.getBody().getCodigo()).isEqualTo(24);

        // 2. MethodArgumentNotValidException
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "numeroCuenta", "Código 7: No existe una cuenta con ese numero"));
        MethodParameter param = new MethodParameter(this.getClass().getDeclaredMethod("globalExceptionHandler_centralizaTodasLasExcepcionesEnUnSoloHandler"), -1);
        MethodArgumentNotValidException manv = new MethodArgumentNotValidException(param, binding);
        assertThat(handler.handleException(manv).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 3. ConstraintViolationException
        ConstraintViolation<?> cv = mock(ConstraintViolation.class);
        jakarta.validation.Path cvPath = mock(jakarta.validation.Path.class);
        when(cvPath.iterator()).thenReturn(Collections.emptyIterator());
        when(cv.getPropertyPath()).thenReturn(cvPath);
        when(cv.getMessage()).thenReturn("Código 11: Cliente inactivo");
        ConstraintViolationException cve = new ConstraintViolationException(Set.of(cv));
        assertThat(handler.handleException(cve).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 4. HttpMessageNotReadableException con distintos tipos
        for (Class<?> targetType : List.of(LocalDate.class, Integer.class, String.class, Boolean.class)) {
            MismatchedInputException mie = MismatchedInputException.from(null, targetType, "bad");
            mie.prependPath(new JsonMappingException.Reference(null, "campoPrueba"));
            HttpMessageNotReadableException hmnr = new HttpMessageNotReadableException("bad", mie, (HttpInputMessage) null);
            assertThat(handler.handleException(hmnr).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(handler.handleException(new HttpMessageNotReadableException("raw", (HttpInputMessage) null)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // 5. Request malformado e integridad
        assertThat(handler.handleException(new MissingServletRequestParameterException("a", "String")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleException(new DataIntegrityViolationException("dup")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // 6. Feign y RetryableException
        Request feignReq = Request.create(Request.HttpMethod.POST, "/tx", Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        assertThat(handler.handleException(new RetryableException(504, "t", Request.HttpMethod.POST, new SocketTimeoutException(), (Date) null, feignReq)).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(handler.handleException(new RetryableException(503, "c", Request.HttpMethod.POST, new RuntimeException(), (Date) null, feignReq)).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(handler.handleException(new FeignException.Unauthorized("401", feignReq, null, null)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(handler.handleException(new FeignException.BadGateway("502", feignReq, null, null)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        // 7. HTTP y general
        assertThat(handler.handleException(new NoResourceFoundException(HttpMethod.GET, "/404")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleException(new HttpRequestMethodNotSupportedException("DELETE")).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(handler.handleException(new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON))).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(handler.handleException(new RuntimeException("boom")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void controladores_ejecutanTodosLosEndpointsCorrectamente() {
        // 1. AdminController
        AdminGuard adminGuardMock = mock(AdminGuard.class);
        ClienteService clienteServiceMock = mock(ClienteService.class);
        AdminController adminController = new AdminController(adminGuardMock, clienteServiceMock);
        AdminClienteRequest adminReq = new AdminClienteRequest();
        adminReq.setRol(Roles.ADMIN);
        when(adminGuardMock.exigirAdmin("Bearer adm")).thenReturn(1);
        when(clienteServiceMock.creaClienteConRol(adminReq, Roles.ADMIN)).thenReturn(new ClienteResponse());
        assertThat(adminController.crearComoAdmin("Bearer adm", adminReq).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. AuthController
        AuthController authController = new AuthController(usuarioService);
        LoginRequest loginReq = new LoginRequest();
        when(usuarioService.login(loginReq)).thenReturn(new LoginResponse());
        when(usuarioService.logout("tok")).thenReturn(new GenericResponse());
        assertThat(authController.login(loginReq).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authController.logout("Bearer tok").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authController.logout("tok").getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. ClienteController
        ClienteController clienteController = new ClienteController(clienteServiceMock, seguridadGuard, adminGuardMock);
        when(clienteServiceMock.creaCliente(any())).thenReturn(new ClienteResponse());
        when(clienteServiceMock.actualizaCliente(any())).thenReturn(new ClienteResponse());
        when(clienteServiceMock.eliminaCliente(any())).thenReturn(new GenericResponse());
        when(clienteServiceMock.reactivarCliente(any())).thenReturn(new GenericResponse());
        when(clienteServiceMock.buscarDinamico(any(), any())).thenReturn(new ListaClientesResponse());

        assertThat(clienteController.crearCliente(new ClienteRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(clienteController.actualizarCliente("Bearer ok", new ClienteActualizaRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clienteController.eliminarCliente("Bearer ok", new ClienteBajaRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clienteController.reactivarCliente("Bearer adm", new ClienteReactivarRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clienteController.buscarDinamico("Bearer ok", new ClienteSearchRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. CuentaController
        CuentaService cuentaServiceMock = mock(CuentaService.class);
        CuentaController cuentaController = new CuentaController(cuentaServiceMock);
        CuentaConsultaRequest consultaReq = new CuentaConsultaRequest();
        consultaReq.setNumeroCuenta("123456789012345678");
        when(cuentaServiceMock.obtenerCuentaPorNumero(any())).thenReturn(new CuentaResponse());
        when(cuentaServiceMock.obtenerCuentasActivas()).thenReturn(new ListaCuentasResponse());
        when(cuentaServiceMock.consultarSaldo(any())).thenReturn(new SaldoResponse());
        when(cuentaServiceMock.abonarSaldo(any())).thenReturn(new SaldoResponse());

        assertThat(cuentaController.obtenerCuenta(consultaReq).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuentaController.obtenerActivas().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuentaController.consultarSaldo(consultaReq).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cuentaController.abonarSaldo(new CuentaAbonoRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. PagoController
        PagoService pagoServiceMock = mock(PagoService.class);
        PagoController pagoController = new PagoController(pagoServiceMock);
        when(pagoServiceMock.enviarPin2fa(any())).thenReturn(new Pin2faResponse());
        when(pagoServiceMock.realizarPago(any())).thenReturn(new PagoResponse());
        assertThat(pagoController.enviarPin2fa(new SolicitudPin2faRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pagoController.realizarPago(new PagoRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. ProductoController
        ProductoService productoServiceMock = mock(ProductoService.class);
        ProductoController productoController = new ProductoController(productoServiceMock);
        when(productoServiceMock.obtenerProductos()).thenReturn(Collections.emptyList()).thenReturn(List.of(new ProductoData()));
        when(productoServiceMock.sincronizarProductos()).thenReturn(5);
        assertThat(productoController.getProductos().getBody().getCodigo()).isNotZero();
        assertThat(productoController.getProductos().getBody().getCodigo()).isZero();
        assertThat(productoController.forceSyncProductos().getBody().getData()).isEqualTo(5);
    }

    @Test
    void configuracionSeguridadEntidadesYModelos_coberturaCompleta() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(LLAVE_JWT_B64, 15L);
        String jwtAdmin = jwtUtil.generar(1, "h1", Roles.ADMIN);
        String jwtUser = jwtUtil.generar(2, "h2", Roles.USUARIO_NORMAL);

        // AdminGuard
        AdminGuard adminGuard = new AdminGuard(jwtUtil);
        assertThat(adminGuard.exigirAdmin("Bearer " + jwtAdmin)).isEqualTo(1);
        assertThrows(ValidacionException.class, () -> adminGuard.exigirAdmin(null));
        String headerUser = "Bearer " + jwtUser;
        assertThrows(ValidacionException.class, () -> adminGuard.exigirAdmin(headerUser));
        assertThrows(ValidacionException.class, () -> adminGuard.exigirAdmin("Bearer invalido"));

        // SeguridadGuard
        SeguridadGuard guard = new SeguridadGuard(jwtUtil, clienteRepository);
        guard.validarAccesoClientePorRfc("Bearer " + jwtAdmin, "RFC1");
        Cliente cli2 = new Cliente();
        cli2.setId(2);
        when(clienteRepository.findByRfc("RFC2")).thenReturn(Optional.of(cli2));
        guard.validarAccesoClientePorRfc(headerUser, "RFC2");

        Cliente cliOtro = new Cliente();
        cliOtro.setId(99);
        when(clienteRepository.findByRfc("RFC99")).thenReturn(Optional.of(cliOtro));
        assertThrows(ValidacionException.class, () -> guard.validarAccesoClientePorRfc(headerUser, "RFC99"));
        assertThrows(ValidacionException.class, () -> guard.validarAccesoClientePorRfc(null, "RFC2"));
        assertThrows(ValidacionException.class, () -> guard.validarAccesoClientePorRfc("Bearer malo", "RFC2"));

        InfoToken info = guard.extraerInfoToken("Bearer " + jwtAdmin);
        assertThat(info.esAdmin()).isTrue();
        assertThat(info.getRol()).isEqualTo(Roles.ADMIN);
        assertThat(guard.extraerInfoToken(null)).isNull();
        assertThrows(ValidacionException.class, () -> guard.extraerInfoToken("Bearer malo"));

        // ActividadSesionInterceptor
        ActividadSesionInterceptor interceptor = new ActividadSesionInterceptor(jwtUtil, usuarioService);
        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        HttpServletResponse servletResp = mock(HttpServletResponse.class);
        when(servletReq.getHeader("Authorization")).thenReturn("Bearer " + jwtUser, "Bearer malo", null);
        assertThat(interceptor.preHandle(servletReq, servletResp, new Object())).isTrue();
        assertThat(interceptor.preHandle(servletReq, servletResp, new Object())).isTrue();
        assertThat(interceptor.preHandle(servletReq, servletResp, new Object())).isTrue();

        // WebConfig y OpenApi
        WebConfig webConfig = new WebConfig(interceptor);
        ReflectionTestUtils.setField(webConfig, "allowedOrigins", new String[]{"*"});
        webConfig.addCorsMappings(new CorsRegistry());
        webConfig.addInterceptors(new InterceptorRegistry());

        assertThat(new OpenApi().openAPI()).isNotNull();

        // RedisConfig y ConfigDB
        RedisProperties redisProps = new RedisProperties();
        redisProps.setHost("localhost");
        redisProps.setPort(6379);
        redisProps.setTimeout(Duration.ofMillis(1000));
        redisProps.setConnectTimeout(Duration.ofMillis(1000));
        RedisConfig redisConfig = new RedisConfig(redisProps);
        RedisConnectionFactory connFactory = redisConfig.redisConnectionFactory();
        assertThat(redisConfig.redisTemplate(connFactory)).isNotNull();

        ConfigDB configDB = new ConfigDB();
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.url")).thenThrow(new RuntimeException("db err"));
        assertThat(configDB.sfDatasource(env)).isNull();
        assertThat(configDB.sfEntityManagerFactory(mock(DataSource.class))).isNotNull();
        assertThat(configDB.sfTransactionManager(mock(EntityManagerFactory.class))).isNotNull();

        // Entidades y GestoPagoAuthResponse
        Cliente nuevoCli = new Cliente();
        nuevoCli.setActivo(null);
        nuevoCli.prePersist();
        assertThat(nuevoCli.getFechaRegistro()).isNotNull();
        assertThat(nuevoCli.getActivo()).isTrue();

        Cuenta nuevaCta = new Cuenta();
        nuevaCta.prePersist();
        assertThat(nuevaCta.getFechaApertura()).isNotNull();
        assertThat(nuevaCta.getEstatus()).isEqualTo("ACTIVA");

        TransaccionPago nuevaTx = new TransaccionPago();
        nuevaTx.prePersist();
        assertThat(nuevaTx.getFechaRegistro()).isNotNull();

        Usuario nuevoUsr = new Usuario();
        nuevoUsr.setSesionActiva(null);
        nuevoUsr.setRol(null);
        nuevoUsr.prePersist();
        assertThat(nuevoUsr.getSesionActiva()).isFalse();
        assertThat(nuevoUsr.getRol()).isEqualTo(2);

        GestoPagoToken gpToken = new GestoPagoToken();
        ReflectionTestUtils.invokeMethod(gpToken, "onCreate");
        ReflectionTestUtils.invokeMethod(gpToken, "onUpdate");
        assertThat(gpToken.getFechaCreacion()).isNotNull();
        assertThat(gpToken.getFechaActualizacion()).isNotNull();

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        assertThat(authResp.isTokenValido()).isFalse();
        authResp.setSuccess(true);
        authResp.setToken("INVALID");
        assertThat(authResp.isTokenValido()).isFalse();
        authResp.setToken("jwt-real");
        assertThat(authResp.isTokenValido()).isTrue();
    }

    private ClienteRequest crearClienteRequestBase() {
        ClienteRequest req = new ClienteRequest();
        req.setNombre("JUAN");
        req.setApellidoPaterno("GOMEZ");
        req.setApellidoMaterno("DIAZ");
        req.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        req.setRfc("GODJ900515ABC");
        req.setCurp("GODJ900515HDFR01");
        req.setSexo("H");
        req.setNacionalidad("Mexicana");
        req.setCorreo("juan@correo.com");
        req.setTelefonoMovil("5512345678");
        req.setPassword("Password123!");

        DomicilioRequest dom = new DomicilioRequest();
        dom.setCalle("Av Reforma");
        dom.setNumeroExterior("100");
        dom.setColonia("Centro");
        dom.setMunicipio("Cuauhtemoc");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("06000");
        dom.setPais("México");
        req.setDomicilio(dom);
        return req;
    }
}
