package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.util.HashUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Validador de modelo que verifica antes de entrar a PagoService:
 * - Existencia y estatus activo de la cuenta (18 digitos) y del cliente.
 * - Suficiencia de saldo para el monto solicitado.
 * - Validez y vigencia del PIN 2FA enviado al correo del cliente.
 */
@Component
public class PagoModelValidator implements ConstraintValidator<PagoModelValido, Object> {

    private static final String CAMPO_NUMERO_CUENTA = "numeroCuenta";
    private static final String CAMPO_PIN_2FA = "pin2fa";

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HashUtil hashUtil;

    public PagoModelValidator(
            CuentaRepository cuentaRepository,
            UsuarioRepository usuarioRepository,
            HashUtil hashUtil) {
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.hashUtil = hashUtil;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || cuentaRepository == null) {
            return true;
        }

        if (value instanceof SolicitudPin2faRequest pinRequest) {
            return validarCuentaYCliente(pinRequest.getNumeroCuenta(), context).isPresent();
        }

        if (value instanceof PagoRequest pagoRequest) {
            return validarPagoRequest(pagoRequest, context);
        }

        return true;
    }

    private boolean validarPagoRequest(PagoRequest request, ConstraintValidatorContext context) {
        Optional<Cuenta> cuentaOpt = validarCuentaYCliente(request.getNumeroCuenta(), context);
        if (cuentaOpt.isEmpty()) {
            return false;
        }

        Cuenta cuenta = cuentaOpt.get();
        BigDecimal monto = request.obtenerMontoLimpio();
        if (monto.compareTo(BigDecimal.ZERO) > 0 && cuenta.getSaldo().compareTo(monto) < 0) {
            return rechazar(context, "monto", MensajeError.SALDO_INSUFICIENTE);
        }

        if (usuarioRepository != null && hashUtil != null && request.getPin2fa() != null && !request.getPin2fa().isBlank()) {
            return validarPin2fa(cuenta.getCliente().getId(), request.getPin2fa(), context);
        }

        return true;
    }

    private Optional<Cuenta> validarCuentaYCliente(String numeroCuenta, ConstraintValidatorContext context) {
        if (numeroCuenta == null || !numeroCuenta.matches("^\\d{18}$")) {
            return Optional.empty();
        }

        Optional<Cuenta> cuentaOpt = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuentaOpt.isEmpty()) {
            rechazar(context, CAMPO_NUMERO_CUENTA, MensajeError.CUENTA_NO_ENCONTRADA);
            return Optional.empty();
        }

        Cuenta cuenta = cuentaOpt.get();
        Cliente cliente = cuenta.getCliente();
        if (cliente == null) {
            rechazar(context, CAMPO_NUMERO_CUENTA, MensajeError.CLIENTE_NO_ENCONTRADO);
            return Optional.empty();
        }
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            rechazar(context, CAMPO_NUMERO_CUENTA, MensajeError.CLIENTE_INACTIVO_COMPRA);
            return Optional.empty();
        }
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            rechazar(context, CAMPO_NUMERO_CUENTA, MensajeError.CUENTA_INACTIVA_COMPRA);
            return Optional.empty();
        }

        return Optional.of(cuenta);
    }

    private boolean validarPin2fa(Integer clienteId, String pinIngresado, ConstraintValidatorContext context) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCliente_Id(clienteId);
        if (usuarioOpt.isEmpty()) {
            return rechazar(context, CAMPO_NUMERO_CUENTA, MensajeError.CLIENTE_NO_ENCONTRADO);
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getPin2faHash() == null || usuario.getPin2faExpiracion() == null) {
            return rechazar(context, CAMPO_PIN_2FA, MensajeError.PIN_2FA_NO_SOLICITADO);
        }

        if (LocalDateTime.now(ZoneId.systemDefault()).isAfter(usuario.getPin2faExpiracion())) {
            usuario.setPin2faHash(null);
            usuario.setPin2faExpiracion(null);
            usuarioRepository.save(usuario);
            return rechazar(context, CAMPO_PIN_2FA, MensajeError.PIN_2FA_EXPIRADO);
        }

        String hashIngresado = hashUtil.sha256(pinIngresado);
        if (!hashIngresado.equals(usuario.getPin2faHash())) {
            return rechazar(context, CAMPO_PIN_2FA, MensajeError.PIN_2FA_INVALIDO);
        }

        return true;
    }

    private boolean rechazar(ConstraintValidatorContext context, String campo, MensajeError error) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error.getMensaje())
                .addPropertyNode(campo)
                .addConstraintViolation();
        return false;
    }
}
