package com.proyecto.servicios.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de enviar el PIN de autenticacion 2FA al correo electronico del cliente.
 */
@Service
@Slf4j
public class Correo2faService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public boolean enviarPinPorCorreo(String correoDestino, String nombreCliente, String pin) {
        if (mailSender == null || remitente == null || remitente.isBlank()) {
            log.info("Envio de PIN 2FA registrado para el correo del cliente (SMTP no configurado en entorno local)");
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(correoDestino);
            mensaje.setSubject("Código de verificación 2FA - Autorización de Pago");
            mensaje.setText("Hola " + nombreCliente + ",\n\n"
                    + "Tu código PIN de autenticación (2FA) para autorizar tu pago o recarga es: " + pin + "\n\n"
                    + "Este código tiene una vigencia de 5 minutos y es de un solo uso.\n"
                    + "Si no solicitaste esta operación, ignora este mensaje.");
            mailSender.send(mensaje);
            log.info("Correo con PIN 2FA enviado exitosamente al cliente");
            return true;
        } catch (Exception e) {
            log.warn("No fue posible enviar el correo SMTP en este entorno ({}). Se conserva el PIN 2FA activo.",
                    e.getClass().getSimpleName());
            return false;
        }
    }
}
