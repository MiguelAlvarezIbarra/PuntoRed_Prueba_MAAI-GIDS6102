package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.util.GestoPagoCryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private Correo2faService correo2faService;

    @Mock
    private SeguridadGuard seguridadGuard;

    @Spy
    private HashUtil hashUtil = new HashUtil();

    @Spy
    private GestoPagoCryptoUtil gestoPagoCryptoUtil = new GestoPagoCryptoUtil(
            "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=",
            "p5ldmBPdd/9pjC0bDC/nSg=="
    );

    @InjectMocks
    private PagoServiceImpl pagoService;

    private Cliente clienteActivo;
    private Cuenta cuentaActiva;
    private Usuario usuario;
    private GestoPagoToken tokenActivo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(pagoService, "upcDefault", "0");
        ReflectionTestUtils.setField(pagoService, "expiracionPinMinutos", 5L);
        ReflectionTestUtils.setField(pagoService, "mostrarPinEnRespuesta", true);

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
    void realizarPago_clienteInactivo_lanzaExcepcion() {
        clienteActivo.setActivo(false);
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));

        PagoRequest request = construirRequestRecarga();

        assertThrows(ClienteInactivoException.class, () -> pagoService.realizarPago(request, null));
        verify(gestoPagoServiceClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    void realizarPago_saldoInsuficiente_lanzaExcepcion() {
        cuentaActiva.setSaldo(new BigDecimal("5.00"));
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));

        PagoRequest request = construirRequestRecarga(); // monto = 10.00

        assertThrows(SaldoInsuficienteException.class, () -> pagoService.realizarPago(request, null));
        verify(gestoPagoServiceClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    void realizarPago_pin2faIncorrecto_lanzaExcepcion() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));

        PagoRequest request = construirRequestRecarga();
        request.setPin2fa("999999");

        assertThrows(ValidacionException.class, () -> pagoService.realizarPago(request, null));
        verify(gestoPagoServiceClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    void realizarPago_recargaExitosaCodigo01_descuentaSaldoYRegresaPagoExitoso() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));

        String xmlExito = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE><ID_TX>945814225</ID_TX><NUM_AUTORIZACION>100092242</NUM_AUTORIZACION>"
                + "<FECHA>23/Oct/2026 18:18:13</FECHA><MONTO>10.0</MONTO>"
                + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO>"
                + "<REFERENCIA>5512345678</REFERENCIA></MENSAJE></RESPONSE>";

        when(gestoPagoServiceClient.sendTx(eq("Bearer jwt-token-gestopago"), anyString())).thenReturn(xmlExito);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            tx.setId(77);
            return tx;
        });

        PagoRequest request = construirRequestRecarga();
        PagoResponse response = pagoService.realizarPago(request, null);

        assertThat(response.getCodigo()).isZero();
        assertThat(response.getMensaje()).isEqualTo("01: Pago Exitoso");
        assertThat(response.getPago().getCodigoGestoPago()).isEqualTo("01");
        assertThat(response.getPago().getSaldoActual()).isEqualByComparingTo(new BigDecimal("490"));
        assertThat(cuentaActiva.getSaldo()).isEqualByComparingTo(new BigDecimal("490"));
        verify(cuentaRepository).save(cuentaActiva);
    }

    @Test
    void enviarPin2fa_generaYGuardaPinParaClienteActivo() {
        when(cuentaRepository.findByNumeroCuenta(CUENTA_18_DIGITOS)).thenReturn(Optional.of(cuentaActiva));
        when(usuarioRepository.findByCliente_Id(10)).thenReturn(Optional.of(usuario));

        SolicitudPin2faRequest req = new SolicitudPin2faRequest();
        req.setNumeroCuenta(CUENTA_18_DIGITOS);

        Pin2faResponse resp = pagoService.enviarPin2fa(req, null);

        assertThat(resp.getCodigo()).isZero();
        assertThat(resp.getPin2fa()).matches("^\\d{6}$");
        verify(usuarioRepository).save(usuario);
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
