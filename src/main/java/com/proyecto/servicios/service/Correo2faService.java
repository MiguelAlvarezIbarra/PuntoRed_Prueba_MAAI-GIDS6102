package com.proyecto.servicios.service;

import com.proyecto.servicios.util.HashUtil;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de hashear y enviar el PIN de autenticacion 2FA al correo electronico del cliente.
 * Soporta APIs HTTP HTTPS (Brevo / Resend, ideales para Render Free donde SMTP 587 suele estar bloqueado)
 * y JavaMailSender SMTP tradicional, con plantilla HTML responsiva para clientes moviles y web.
 */
@Service
@Slf4j
public class Correo2faService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String RESEND_DEFAULT_FROM = "PuntoRed Seguridad <onboarding@resend.dev>";

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

    @Value("${seguridad.2fa.expiracion-minutos:5}")
    private long expiracionMinutos = 5;

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

        String asunto = "🔐 Código de verificación 2FA PuntoRed: " + pin;
        String cuerpoTexto = construirMensajeTexto(nombreCliente, pin);
        String cuerpoHtml = construirMensajeHtml(nombreCliente, pin);

        if (tieneTexto(brevoApiKey)) {
            if (enviarPorBrevo(correoDestino, nombreCliente, asunto, cuerpoTexto, cuerpoHtml)) {
                return true;
            }
        }

        if (tieneTexto(resendApiKey)) {
            if (enviarPorResend(correoDestino, asunto, cuerpoTexto, cuerpoHtml)) {
                return true;
            }
        }

        if (mailSender == null || !tieneTexto(remitente)) {
            log.info("Envio de PIN 2FA registrado sin salida de email (sin SMTP/Brevo/Resend configurado)");
            return false;
        }

        return enviarPorSmtp(correoDestino.trim(), asunto, cuerpoTexto, cuerpoHtml);
    }

    private boolean enviarPorSmtp(String correoDestino, String asunto, String cuerpoTexto, String cuerpoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage != null) {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                helper.setFrom(remitente.trim());
                helper.setTo(correoDestino);
                helper.setSubject(asunto);
                helper.setText(cuerpoTexto, cuerpoHtml);
                mailSender.send(mimeMessage);
                log.info("Correo HTML SMTP con PIN 2FA enviado exitosamente al cliente");
                return true;
            }

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente.trim());
            mensaje.setTo(correoDestino);
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

    private boolean enviarPorBrevo(
            String correoDestino,
            String nombreCliente,
            String asunto,
            String cuerpoTexto,
            String cuerpoHtml) {
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
            body.put("htmlContent", cuerpoHtml);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    BREVO_API_URL, new HttpEntity<>(body, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("Correo HTML con PIN 2FA enviado exitosamente via Brevo API");
            }
            return ok;
        } catch (Exception e) {
            log.warn("Fallo envio de correo via Brevo API ({}).", e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean enviarPorResend(String correoDestino, String asunto, String cuerpoTexto, String cuerpoHtml) {
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
            body.put("html", cuerpoHtml);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("Correo HTML con PIN 2FA enviado exitosamente via Resend API");
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
        long mins = expiracionMinutos > 0 ? expiracionMinutos : 5;
        return "Hola " + saludo + ",\n\n"
                + "Tu código PIN de autenticación (2FA) para autorizar tu pago o recarga en PuntoRed es: " + pin + "\n\n"
                + "Vigencia: " + mins + " minutos (un solo uso).\n"
                + "No compartas este código con nadie.\n"
                + "Si no solicitaste esta operación, ignora este mensaje.";
    }

    String construirMensajeHtml(String nombreCliente, String pin) {
        String nombreSeguro = escapeHtml(tieneTexto(nombreCliente) ? nombreCliente.trim() : "Cliente");
        String pinSeguro = escapeHtml(pin != null ? pin.trim() : "");
        long mins = expiracionMinutos > 0 ? expiracionMinutos : 5;

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Código 2FA PuntoRed</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f1f5f9;padding:28px 14px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:540px;background-color:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 8px 24px rgba(15,23,42,0.08);">
                          <!-- Cabecera Fintech -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f172a 0%%,#1e3a8a 55%%,#2563eb 100%%);padding:26px 28px;text-align:left;">
                              <div style="display:inline-block;background-color:rgba(255,255,255,0.14);color:#e0f2fe;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;padding:5px 11px;border-radius:999px;margin-bottom:10px;">
                                PuntoRed • Seguridad H2H V2
                              </div>
                              <h1 style="margin:0;font-size:22px;line-height:1.3;font-weight:800;color:#ffffff;">
                                Código de verificación 2FA
                              </h1>
                              <p style="margin:6px 0 0 0;font-size:13px;color:#bfdbfe;">
                                Autorización de pago o recarga electrónica
                              </p>
                            </td>
                          </tr>

                          <!-- Cuerpo -->
                          <tr>
                            <td style="padding:28px 28px 20px 28px;">
                              <p style="margin:0 0 12px 0;font-size:15px;line-height:1.6;color:#1e293b;">
                                Hola <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 22px 0;font-size:14px;line-height:1.6;color:#475569;">
                                Recibimos una solicitud para autorizar una operación desde tu cuenta. Ingresa el siguiente PIN de seguridad de 6 dígitos en la app para confirmar:
                              </p>

                              <!-- Tarjeta PIN -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#eff6ff;border:1.5px dashed #60a5fa;border-radius:14px;margin-bottom:20px;">
                                <tr>
                                  <td align="center" style="padding:22px 16px;">
                                    <div style="font-size:11px;font-weight:700;letter-spacing:1.2px;text-transform:uppercase;color:#1d4ed8;margin-bottom:8px;">
                                      Tu PIN temporal de autorización
                                    </div>
                                    <div style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:800;letter-spacing:9px;color:#0f172a;line-height:1.2;">
                                      %s
                                    </div>
                                  </td>
                                </tr>
                              </table>

                              <!-- Chips informativos -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom:22px;">
                                <tr>
                                  <td width="50%%" style="padding-right:6px;">
                                    <div style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:10px 12px;text-align:center;">
                                      <div style="font-size:11px;color:#64748b;text-transform:uppercase;font-weight:600;">Vigencia</div>
                                      <div style="font-size:14px;font-weight:700;color:#0f172a;margin-top:2px;">%d minutos</div>
                                    </div>
                                  </td>
                                  <td width="50%%" style="padding-left:6px;">
                                    <div style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:10px 12px;text-align:center;">
                                      <div style="font-size:11px;color:#64748b;text-transform:uppercase;font-weight:600;">Uso</div>
                                      <div style="font-size:14px;font-weight:700;color:#0f172a;margin-top:2px;">Único e intransferible</div>
                                    </div>
                                  </td>
                                </tr>
                              </table>

                              <!-- Alerta de seguridad -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#fffbeb;border-left:4px solid #f59e0b;border-radius:8px;">
                                <tr>
                                  <td style="padding:12px 14px;font-size:12.5px;line-height:1.5;color:#92400e;">
                                    <strong>Consejo de seguridad:</strong> Nunca compartas este código por WhatsApp, llamada ni mensajes. Si no solicitaste este movimiento, ignora este correo y revisa tus movimientos en la app.
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:16px 28px;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#64748b;">
                                Mensaje automático de seguridad • <strong>PuntoRed App Móvil</strong>
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nombreSeguro, pinSeguro, mins);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }
}
