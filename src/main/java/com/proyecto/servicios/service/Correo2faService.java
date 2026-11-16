package com.proyecto.servicios.service;

import com.proyecto.servicios.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de hashear y enviar el PIN de autenticacion 2FA al correo electronico del cliente.
 * Soporta APIs HTTP HTTPS (Brevo / Resend, ideales para Render Free donde SMTP 587 suele estar bloqueado)
 * y JavaMailSender SMTP tradicional.
 */
@Service
@Slf4j
public class Correo2faService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String RESEND_DEFAULT_FROM = "PuntoRed 2FA <onboarding@resend.dev>";

    private final JavaMailSender mailSender;
    private final HashUtil hashUtil;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:}")
    private String remitente;

    @Value("${seguridad.2fa.correo.remitente:}")
    private String remitenteHttp;

    @Value("${seguridad.2fa.correo.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${seguridad.2fa.correo.resend-api-key:}")
    private String resendApiKey;

    public Correo2faService(ObjectProvider<JavaMailSender> mailSenderProvider, HashUtil hashUtil) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.hashUtil = hashUtil;
        this.restTemplate = crearRestTemplate();
    }

    private static RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(7000);
        return new RestTemplate(factory);
    }

    public String hashearPin(String pin) {
        return hashUtil.sha256(pin);
    }

    public boolean enviarPinPorCorreo(String correoDestino, String nombreCliente, String pin) {
        if (correoDestino == null || !correoDestino.contains("@")) {
            log.warn("No se envio PIN 2FA: el cliente no tiene un correo valido registrado");
            return false;
        }

        String asunto = "Código de verificación 2FA - Autorización de Pago";
        String cuerpoTexto = construirMensajeTexto(nombreCliente, pin);

        if (tieneTexto(brevoApiKey)) {
            if (enviarPorBrevo(correoDestino, nombreCliente, asunto, cuerpoTexto)) {
                return true;
            }
        }

        if (tieneTexto(resendApiKey)) {
            if (enviarPorResend(correoDestino, asunto, cuerpoTexto)) {
                return true;
            }
        }

        if (mailSender == null || !tieneTexto(remitente)) {
            log.info("Envio de PIN 2FA registrado sin salida de email (sin SMTP/Brevo/Resend configurado)");
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente.trim());
            mensaje.setTo(correoDestino.trim());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpoTexto);
            mailSender.send(mensaje);
            log.info("Correo SMTP con PIN 2FA enviado exitosamente al cliente");
            return true;
        } catch (Exception e) {
            log.warn("No fue posible enviar el correo SMTP en este entorno ({}). Se conserva el PIN 2FA activo.",
                    e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean enviarPorBrevo(String correoDestino, String nombreCliente, String asunto, String cuerpoTexto) {
        String senderEmail = resolverRemitente();
        if (!tieneTexto(senderEmail)) {
            log.warn("BREVO_API_KEY configurado pero falta MAIL_FROM o MAIL_USERNAME como remitente verificado");
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey.trim());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sender", Map.of("name", "PuntoRed Seguridad", "email", senderEmail));
            body.put("to", List.of(Map.of("email", correoDestino.trim(),
                    "name", tieneTexto(nombreCliente) ? nombreCliente.trim() : correoDestino.trim())));
            body.put("subject", asunto);
            body.put("textContent", cuerpoTexto);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    BREVO_API_URL, new HttpEntity<>(body, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("Correo con PIN 2FA enviado exitosamente via Brevo API");
            }
            return ok;
        } catch (Exception e) {
            log.warn("Fallo envio de correo via Brevo API ({}).", e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean enviarPorResend(String correoDestino, String asunto, String cuerpoTexto) {
        String sender = resolverRemitente();
        String fromHeader = tieneTexto(sender) ? sender : RESEND_DEFAULT_FROM;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", fromHeader);
            body.put("to", List.of(correoDestino.trim()));
            body.put("subject", asunto);
            body.put("text", cuerpoTexto);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("Correo con PIN 2FA enviado exitosamente via Resend API");
            }
            return ok;
        } catch (Exception e) {
            log.warn("Fallo envio de correo via Resend API ({}).", e.getClass().getSimpleName());
            return false;
        }
    }

    private String resolverRemitente() {
        if (tieneTexto(remitenteHttp)) {
            return remitenteHttp.trim();
        }
        if (tieneTexto(remitente)) {
            return remitente.trim();
        }
        return "";
    }

    private String construirMensajeTexto(String nombreCliente, String pin) {
        String saludo = tieneTexto(nombreCliente) ? nombreCliente.trim() : "cliente";
        return "Hola " + saludo + ",\n\n"
                + "Tu código PIN de autenticación (2FA) para autorizar tu pago o recarga es: " + pin + "\n\n"
                + "Este código tiene una vigencia de 5 minutos y es de un solo uso.\n"
                + "Si no solicitaste esta operación, ignora este mensaje.";
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }
}
