package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resultado normalizado de una peticion HTTP hacia Puntored H2H V2,
 * conservando estado HTTP, banderas de timeout/red y el JSON devuelto.
 */
public record PuntoredHttpResult(
        int httpStatus,
        boolean timedOut,
        boolean transportError,
        JsonNode body,
        String rawBody
) {

    public boolean esTransaccionExitosa() {
        return !timedOut
                && !transportError
                && httpStatus >= 200
                && httpStatus < 300
                && body != null
                && body.path("state").asBoolean(false)
                && "00".equalsIgnoreCase(body.path("code").asText(""));
    }

    public boolean esEstadoIncierto() {
        return timedOut || transportError || httpStatus == 502 || httpStatus == 503 || httpStatus == 504;
    }

    public String obtenerCodigo() {
        if (body != null && body.hasNonNull("code")) {
            return body.get("code").asText();
        }
        return timedOut ? "TIMEOUT" : String.valueOf(httpStatus);
    }

    public String obtenerMensaje() {
        if (body != null && body.hasNonNull("message")) {
            String msg = body.get("message").asText();
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        }
        if (timedOut) {
            return "Tiempo de espera agotado al esperar respuesta de PuntoRed";
        }
        if (transportError) {
            return "Error de comunicacion de red con PuntoRed";
        }
        return "Respuesta HTTP " + httpStatus + " de PuntoRed";
    }
}
