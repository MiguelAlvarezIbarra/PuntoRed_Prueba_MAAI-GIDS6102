package com.proyecto.servicios.model;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validacion integral a nivel de modelo para las peticiones de pagos y envio de PIN 2FA.
 * Garantiza que cuenta, cliente, saldo, bandera de telefono/referencia y PIN 2FA sean
 * validos desde el propio modelo antes de invocar la capa de servicio de pagos.
 */
@Documented
@Constraint(validatedBy = PagoModelValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PagoModelValido {

    String message() default "Código 1: Datos de pago no válidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
