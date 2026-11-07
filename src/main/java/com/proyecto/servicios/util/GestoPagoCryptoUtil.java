package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.SenTxRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Cifra y verifica las peticiones SenTx (sendTx.do) de GestoPago.
 * Utiliza AES/GCM/NoPadding con IV aleatorio por operacion en Base64
 * dentro del parametro 'signed', autenticado con el Bearer Token.
 */
@Component
public class GestoPagoCryptoUtil {

    private static final String ALGORITMO_GESTOPAGO = "AES/GCM/NoPadding";
    private static final int TAMANO_TAG_BITS = 128;
    private static final int TAMANO_IV_BYTES = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom random = new SecureRandom();

    public GestoPagoCryptoUtil(
            @Value("${gestopago.tx.aes-key:HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=}") String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Construye el JSON de SenTx y lo cifra en Base64 para enviarlo a GestoPago.
     */
    public String cifrarSenTx(SenTxRequest request) {
        JSONObject json = new JSONObject();
        json.put("idProducto", request.getIdProducto());
        json.put("idServicio", request.getIdServicio());
        json.put("telefono", request.getTelefono() != null ? request.getTelefono() : "");
        json.put("referencia", request.getReferencia());
        json.put("horaLocal", request.getHoraLocal());
        BigDecimal montoLimpio = request.getMontoPago().stripTrailingZeros();
        if (montoLimpio.scale() < 0) {
            montoLimpio = montoLimpio.setScale(0);
        }
        json.put("montoPago", montoLimpio);
        json.put("upc", request.getUpc());
        json.put("unidad", request.getUnidad());

        return cifrarTexto(json.toString());
    }

    public String cifrarTexto(String jsonPlano) {
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO_GESTOPAGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(jsonPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(encrypted, 0, resultado, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible cifrar la peticion SenTx de GestoPago", e);
        }
    }

    public String descifrarTexto(String signedBase64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(signedBase64);
            byte[] iv = new byte[TAMANO_IV_BYTES];
            byte[] encrypted = new byte[decoded.length - TAMANO_IV_BYTES];
            System.arraycopy(decoded, 0, iv, 0, TAMANO_IV_BYTES);
            System.arraycopy(decoded, TAMANO_IV_BYTES, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO_GESTOPAGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ValidacionException(MensajeError.SENTX_CIFRADO_INVALIDO);
        }
    }

    /**
     * Verifica y compara que el SenTx cifrado sea integro respecto a los parametros
     * de la transaccion y que el token de GestoPago sea valido antes de ejecutar sendTx.do.
     */
    public boolean verificarSenTxConToken(String senTxCifrado, SenTxRequest original, String token) {
        if (senTxCifrado == null || senTxCifrado.isBlank()
                || token == null || token.isBlank() || "INVALID".equalsIgnoreCase(token)) {
            throw new ValidacionException(MensajeError.SENTX_CIFRADO_INVALIDO);
        }

        String jsonDescifrado = descifrarTexto(senTxCifrado);
        JSONObject json = new JSONObject(jsonDescifrado);

        boolean coincide = Objects.equals(json.optInt("idProducto"), original.getIdProducto())
                && Objects.equals(json.optInt("idServicio"), original.getIdServicio())
                && Objects.equals(json.optString("referencia"), original.getReferencia())
                && Objects.equals(json.optString("horaLocal"), original.getHoraLocal())
                && Objects.equals(json.optString("upc"), original.getUpc())
                && Objects.equals(json.optString("unidad"), original.getUnidad());

        if (!coincide) {
            throw new ValidacionException(MensajeError.SENTX_CIFRADO_INVALIDO);
        }
        return true;
    }
}
