package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.ClienteActualizaRequest;
import com.proyecto.servicios.model.ClienteRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ValidadorRfcCurp {

    private ValidadorRfcCurp() {
    }

    public static void validar(ClienteRequest request) {
        String baseCalculada = calcularBaseRfcCurp(
                request.getNombre(),
                request.getApellidoPaterno(),
                request.getApellidoMaterno(),
                request.getFechaNacimiento()
        );

        validarPrefijoRfc(request.getRfc(), baseCalculada);

        if (request.getCurp() != null && request.getCurp().length() >= 11) {
            String curpInput = request.getCurp().toUpperCase();
            String baseCurpInput = curpInput.substring(0, 10);
            if (!baseCurpInput.equals(baseCalculada)) {
                throw new ValidacionException(MensajeError.CURP_NO_COINCIDE.formatear(baseCurpInput, baseCalculada));
            }

            String sexoEsperado = request.getSexo().toUpperCase();
            String sexoCurp = curpInput.substring(10, 11);
            if (!sexoEsperado.equals(sexoCurp)) {
                throw new ValidacionException(MensajeError.SEXO_CURP_NO_COINCIDE.formatear(sexoCurp, sexoEsperado));
            }
        }
    }

    public static void validarActualizacion(ClienteActualizaRequest request, LocalDate fechaNacimientoRegistrada) {
        if (fechaNacimientoRegistrada == null) {
            return;
        }
        String baseCalculada = calcularBaseRfcCurp(
                request.getNombre(),
                request.getApellidoPaterno(),
                request.getApellidoMaterno(),
                fechaNacimientoRegistrada
        );
        validarPrefijoRfc(request.getRfc(), baseCalculada);
    }

    private static void validarPrefijoRfc(String rfc, String baseCalculada) {
        if (rfc != null && rfc.length() >= 10) {
            String baseRfcInput = rfc.toUpperCase().substring(0, 10);
            if (!baseRfcInput.equals(baseCalculada)) {
                throw new ValidacionException(MensajeError.RFC_NO_COINCIDE.formatear(baseRfcInput, baseCalculada));
            }
        }
    }

    private static String calcularBaseRfcCurp(String nombre, String paterno, String materno, LocalDate fechaNac) {
        String nombreLimpio = limpiarCadena(nombre);
        String paternoLimpio = limpiarCadena(paterno);
        String maternoLimpio = limpiarCadena(materno);

        String primeraLetra = paternoLimpio.isEmpty() ? "X" : paternoLimpio.substring(0, 1);
        String primeraVocalInterna = obtenerPrimeraVocalInterna(paternoLimpio);
        String terceraLetra = maternoLimpio.isEmpty() ? "X" : maternoLimpio.substring(0, 1);
        String cuartaLetra = obtenerLetraNombre(nombreLimpio);
        String fechaFormateada = fechaNac.format(DateTimeFormatter.ofPattern("yyMMdd"));

        return primeraLetra + primeraVocalInterna + terceraLetra + cuartaLetra + fechaFormateada;
    }

    private static String obtenerPrimeraVocalInterna(String paterno) {
        for (int i = 1; i < paterno.length(); i++) {
            char c = paterno.charAt(i);
            if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') {
                return String.valueOf(c);
            }
        }
        return "X";
    }

    private static String obtenerLetraNombre(String nombre) {
        String[] nombres = nombre.split(" ");
        String nombreUsar = nombres[0];
        if (nombres.length > 1 && ("JOSE".equals(nombreUsar) || "MARIA".equals(nombreUsar))) {
            nombreUsar = nombres[1];
        }
        return nombreUsar.isEmpty() ? "X" : nombreUsar.substring(0, 1);
    }

    private static String limpiarCadena(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ü", "U");
    }
}
