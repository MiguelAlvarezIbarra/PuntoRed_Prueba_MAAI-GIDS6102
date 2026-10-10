package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.ClienteRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ValidadorRfcCurp {

    public static void validar(ClienteRequest request) {
        String baseCalculada = calcularBaseRfcCurp(
                request.getNombre(),
                request.getApellidoPaterno(),
                request.getApellidoMaterno(),
                request.getFechaNacimiento()
        );

        if (request.getRfc() != null) {
            String rfcInput = request.getRfc().toUpperCase();
            if (rfcInput.length() >= 10) {
                String baseRfcInput = rfcInput.substring(0, 10);
                if (!baseRfcInput.equals(baseCalculada)) {
                    throw new ValidacionException("Las primeras 10 posiciones del RFC (" + baseRfcInput + 
                            ") no coinciden con los datos del cliente. Deberían ser: " + baseCalculada);
                }
            }
        }

        if (request.getCurp() != null) {
            String curpInput = request.getCurp().toUpperCase();
            if (curpInput.length() >= 11) {
                String baseCurpInput = curpInput.substring(0, 10);
                if (!baseCurpInput.equals(baseCalculada)) {
                    throw new ValidacionException("Las primeras 10 posiciones de la CURP (" + baseCurpInput + 
                            ") no coinciden con los datos del cliente. Deberían ser: " + baseCalculada);
                }
                
                String sexoEsperado = request.getSexo().toUpperCase();
                String sexoCurp = curpInput.substring(10, 11);
                if (!sexoEsperado.equals(sexoCurp)) {
                    throw new ValidacionException("El sexo en la CURP (" + sexoCurp + 
                            ") no coincide con el sexo del cliente (" + sexoEsperado + ").");
                }
            }
        }
    }

    private static String calcularBaseRfcCurp(String nombre, String paterno, String materno, LocalDate fechaNac) {
        nombre = limpiarCadena(nombre);
        paterno = limpiarCadena(paterno);
        materno = limpiarCadena(materno);

        String primeraLetra = paterno.length() > 0 ? paterno.substring(0, 1) : "X";
        String primeraVocalInterna = "X";
        for (int i = 1; i < paterno.length(); i++) {
            char c = paterno.charAt(i);
            if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') {
                primeraVocalInterna = String.valueOf(c);
                break;
            }
        }

        String terceraLetra = materno.length() > 0 ? materno.substring(0, 1) : "X";

        // Filtrar José o María si hay más nombres (simplificado)
        String[] nombres = nombre.split(" ");
        String nombreUsar = nombres[0];
        if (nombres.length > 1 && (nombreUsar.equals("JOSE") || nombreUsar.equals("MARIA"))) {
            nombreUsar = nombres[1];
        }
        String cuartaLetra = nombreUsar.length() > 0 ? nombreUsar.substring(0, 1) : "X";

        String fechaFormateada = fechaNac.format(DateTimeFormatter.ofPattern("yyMMdd"));

        return primeraLetra + primeraVocalInterna + terceraLetra + cuartaLetra + fechaFormateada;
    }

    private static String limpiarCadena(String input) {
        if (input == null) return "";
        return input.trim().toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U")
                .replace("Ñ", "X");
    }
}
