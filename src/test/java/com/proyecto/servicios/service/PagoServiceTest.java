package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.PagoModelValidator;
import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;
import com.proyecto.servicios.model.gestopago.SenTxRequest;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.util.GestoPagoCryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String CUENTA_18_DIGITOS = "123456789012345678";

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private GestoPagoServiceClient gestoPagoServiceClient;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private final HashUtil hashUtil = new HashUtil();

    private final GestoPagoCryptoUtil gestoPagoCryptoUtil = new GestoPagoCryptoUtil(
            "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA="
    );

    private Correo2faService correo2faService;
    private PagoServiceImpl pagoService;
    private PagoModelValidator pagoModelValidator;

    private Cliente clienteActivo;
    private Cuenta cuentaActiva;
    private Usuario usuario;
    private GestoPagoToken tokenActivo;

    @BeforeEach
    void setUp() {
        lenient().when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        correo2faService = new Correo2faService(mailSenderProvider, hashUtil);
        ReflectionTestUtils.setField(correo2faService, "remitente", "no-reply@puntored.com");

        pagoService = new PagoServiceImpl(
                cuentaRepository,
                usuarioRepository,
                transaccionPagoRepository,
                gestoPagoTokenService,
                gestoPagoServiceClient,
                gestoPagoCryptoUtil,
                correo2faService
        );
        ReflectionTestUtils.setField(pagoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoService, "upcDefault", "0");
        ReflectionTestUtils.setField(pagoService, "expiracionPinMinutos", 5L);
        ReflectionTestUtils.setField(pagoService, "mostrarPinEnRespuesta", true);

        pagoModelValidator = new PagoModelValidator(cuentaRepository, usuarioRepository, hashUtil);

        clienteActivo = new Cliente();
        clienteActivo.setId(10);
        clienteActivo.setNombre("Juan");
        clienteActivo.setCorreo("juan@correo.com");
        clienteActivo.setActivo(true);

        cuentaActiva = new Cuenta();
        cuentaActiva.setId(1);
        cuentaActiva.setCliente(clienteActivo);
        cuentaActiva.setNumeroCuenta(CUENTA_18_DIGITOS);
        cuentaActiva.setSaldo(new BigDecimal("500.00"));
        cuentaActiva.setEstatus("ACTIVA");

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setCliente(clienteActivo);
        usuario.setPin2faHash(hashUtil.sha256("123456"));
        usuario.setPin2faExpiracion(LocalDateTime.now().plusMinutes(5));

        tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("jwt-token-gestopago");
    }

    @Test
    void validacionDesdeModelo_bloqueaClienteInactivoCuentaInactivaYSaldoInsuficiente() {
        ConstraintValidatorContext ctx = mockContext();

        // 1. Cliente inactivo bloqueado desde el modelo
        clienteActivo.setActivo(false);
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        PagoRequest request = construirRequestRecarga();
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        // 2. Cuenta inactiva bloqueada desde el modelo
        clienteActivo.setActivo(true);
        cuentaActiva.setEstatus("INACTIVA");
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        // 3. Saldo insuficiente bloqueado desde el modelo
        cuentaActiva.setEstatus("ACTIVA");
        cuentaActiva.setSaldo(new BigDecimal("5.00"));
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        // 4. PIN 2FA incorrecto, expirado o no solicitado bloqueado desde el modelo
        cuentaActiva.setSaldo(new BigDecimal("500.00"));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));
        request.setPin2fa("999999");
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        request.setPin2fa("123456");
        usuario.setPin2faExpiracion(LocalDateTime.now().minusMinutes(1));
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        usuario.setPin2faHash(null);
        assertThat(pagoModelValidator.isValid(request, ctx)).isFalse();

        // 5. Caso valido en el modelo
        usuario.setPin2faHash(hashUtil.sha256("123456"));
        usuario.setPin2faExpiracion(LocalDateTime.now().plusMinutes(5));
        assertThat(pagoModelValidator.isValid(request, ctx)).isTrue();

        SolicitudPin2faRequest pinReq = new SolicitudPin2faRequest();
        pinReq.setNumeroCuenta(CUENTA_18_DIGITOS);
        assertThat(pagoModelValidator.isValid(pinReq, ctx)).isTrue();

        // 6. Validaciones de bandera en el propio PagoRequest
        assertThat(request.isTelefonoValidoParaRecarga()).isTrue();
        assertThat(request.isReferenciaValidaParaPago()).isTrue();
        request.setTelefono("");
        assertThat(request.isTelefonoValidoParaRecarga()).isFalse();
        request.setTipoPago("REFERENCIA");
        request.setReferencia("");
        assertThat(request.isReferenciaValidaParaPago()).isFalse();
    }

    @Test
    void realizarPago_recargaExitosaCodigo01_descuentaSaldoYRegresaPagoExitoso() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));

        String xmlExito = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE><ID_TX>945814225</ID_TX><NUM_AUTORIZACION>100092242</NUM_AUTORIZACION>"
                + "<FECHA>23/Oct/2026 18:18:13</FECHA><MONTO>10.0</MONTO><PIN>9876</PIN>"
                + "<MENSAJE><CODIGO>1</CODIGO><TEXTO>Operacion realizada con exito</TEXTO>"
                + "<LEYENDA>Gracias</LEYENDA><REFERENCIA>5512345678</REFERENCIA></MENSAJE></RESPONSE>";

        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString())).thenReturn(xmlExito);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            tx.setId(77);
            return tx;
        });

        PagoRequest request = construirRequestRecarga();
        PagoResponse response = pagoService.realizarPago(request);

        assertThat(response.getCodigo()).isZero();
        assertThat(response.getMensaje()).isEqualTo("01: Pago Exitoso");
        assertThat(response.getPago().getCodigoGestoPago()).isEqualTo("01");
        assertThat(response.getPago().getSaldoActual()).isEqualByComparingTo(new BigDecimal("490"));
        assertThat(cuentaActiva.getSaldo()).isEqualByComparingTo(new BigDecimal("490"));
        verify(cuentaRepository).save(cuentaActiva);
    }

    @Test
    void realizarPago_respuestaAutorizacionYRechazoProveedor_noDescuentaSaldoCuandoFalla() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));
        when(gestoPagoTokenService.obtenerTokenActual())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(tokenActivo));

        String xmlAutRechazo = "<AUTORIZACION><CODIGO_MENSAJE>05</CODIGO_MENSAJE>"
                + "<MENSAJE>Rechazado por carrier</MENSAJE><ID_TX>11</ID_TX>"
                + "<NUM_AUTORIZACION>0</NUM_AUTORIZACION><FECHA>hoy</FECHA></AUTORIZACION>";

        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString())).thenReturn(xmlAutRechazo);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoRequest request = construirRequestRecarga();
        request.setTipoPago("REFERENCIA");
        request.setReferencia("REF123456");
        request.setUpc("UPC1");
        request.setUnidad("83");

        PagoResponse response = pagoService.realizarPago(request);

        assertThat(response.getCodigo()).isEqualTo(5);
        assertThat(response.getPago().getSaldoActual()).isEqualByComparingTo(new BigDecimal("500"));
        verify(cuentaRepository, never()).save(cuentaActiva);

        // Respuesta XML desconocida, RESPONSE sin MENSAJE, y codigo no numerico
        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString())).thenReturn("<OTRO/>");
        PagoResponse respOtro = pagoService.realizarPago(request);
        assertThat(respOtro.getCodigo()).isEqualTo(99);

        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString()))
                .thenReturn("<RESPONSE><ID_TX>1</ID_TX></RESPONSE>");
        assertThat(pagoService.realizarPago(request).getCodigo()).isEqualTo(99);

        cuentaActiva.setSaldo(new BigDecimal("500.50"));
        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString()))
                .thenReturn("<AUTORIZACION><CODIGO_MENSAJE>ERR</CODIGO_MENSAJE><LEYENDA>   </LEYENDA></AUTORIZACION>");
        assertThat(pagoService.realizarPago(request).getCodigo()).isEqualTo(24);

        cuentaActiva.setSaldo(null);
        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString())).thenReturn(null);
        assertThat(pagoService.realizarPago(request).getCodigo()).isEqualTo(99);
    }

    @Test
    void enviarPin2fa_generaYGuardaPinConYSinSmtp() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));

        SolicitudPin2faRequest req = new SolicitudPin2faRequest();
        req.setNumeroCuenta(CUENTA_18_DIGITOS);

        Pin2faResponse resp = pagoService.enviarPin2fa(req);
        assertThat(resp.getCodigo()).isZero();
        assertThat(resp.getPin2fa()).matches("^\\d{6}$");
        verify(usuarioRepository).save(usuario);

        // Con mostrarPinEnRespuesta = false y envio SMTP exitoso
        ReflectionTestUtils.setField(pagoService, "mostrarPinEnRespuesta", false);
        assertThat(pagoService.enviarPin2fa(req).getPin2fa()).isNull();

        // Simulando fallo de SMTP y correo nulo
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        clienteActivo.setCorreo(null);
        assertThat(pagoService.enviarPin2fa(req).getCorreoDestino()).isEqualTo("***");
        clienteActivo.setCorreo("invalido");
        Pin2faResponse respFallback = pagoService.enviarPin2fa(req);
        assertThat(respFallback.getCorreoDestino()).isEqualTo("***");

        // Sin remitente configurado
        ReflectionTestUtils.setField(correo2faService, "remitente", "");
        assertThat(correo2faService.enviarPinPorCorreo("a@b.com", "Juan", "123456")).isFalse();
    }

    @Test
    void gestoPagoCryptoUtil_cifraDescifraYVerificaSenTx() {
        SenTxRequest senTx = new SenTxRequest(582, 133, "5512345678", "5512345678",
                "20261023180000", new BigDecimal("10.00"), "0", "83");
        String cifrado = gestoPagoCryptoUtil.cifrarSenTx(senTx);
        assertThat(gestoPagoCryptoUtil.verificarSenTxConToken(cifrado, senTx, "jwt-valido")).isTrue();

        assertThrows(ValidacionException.class,
                () -> gestoPagoCryptoUtil.verificarSenTxConToken(cifrado, senTx, "INVALID"));
        assertThrows(ValidacionException.class,
                () -> gestoPagoCryptoUtil.descifrarTexto("no-base64-aes"));
    }

    private ConstraintValidatorContext mockContext() {
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeCtx =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        lenient().when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addPropertyNode(anyString())).thenReturn(nodeCtx);
        lenient().when(nodeCtx.addConstraintViolation()).thenReturn(ctx);
        return ctx;
    }

    private PagoRequest construirRequestRecarga() {
        PagoRequest req = new PagoRequest();
        req.setNumeroCuenta(CUENTA_18_DIGITOS);
        req.setIdServicio(133);
        req.setIdProducto(582);
        req.setMonto(new BigDecimal("10.00"));
        req.setTipoPago("TELEFONO");
        req.setTelefono("5512345678");
        req.setPin2fa("123456");
        return req;
    }
}
