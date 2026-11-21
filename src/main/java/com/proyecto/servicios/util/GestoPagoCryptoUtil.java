package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.SenTxRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Cifra y verifica las peticiones SenTx (sendTx.do / verifyReference.do / confirmTx.do) de GestoPago.
 * Implementa el estandar oficial de la especificacion PuntoRed/GestoPago:
 * AES/CBC/PKCS5Padding con SECRET_KEY e IV en Base64 dentro del parametro 'signed'.
 */
@Component
public class GestoPagoCryptoUtil {

    private static final String ALGORITMO_GESTOPAGO = "AES/CBC/PKCS5Padding";
    private static final int TAMANO_IV_BYTES = 16;

    private final SecretKeySpec secretKey;
    private final IvParameterSpec ivSpec;

    public GestoPagoCryptoUtil(String keyBase64) {
        this(keyBase64, null);
    }

    @Autowired
    public GestoPagoCryptoUtil(
            @Value("${gestopago.tx.aes-key}") String keyBase64,
            @Value("${gestopago.tx.aes-iv:}") String ivBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException("Propiedad gestopago.tx.aes-key no configurada");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
        this.secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] ivBytes;
        if (ivBase64 != null && !ivBase64.isBlank()) {
            ivBytes = Base64.getDecoder().decode(ivBase64.trim());
            if (ivBytes.length != TAMANO_IV_BYTES) {
                throw new IllegalStateException("Propiedad gestopago.tx.aes-iv debe decodificar a 16 bytes");
            }
        } else {
            ivBytes = Arrays.copyOf(keyBytes, TAMANO_IV_BYTES);
        }
        this.ivSpec = new IvParameterSpec(ivBytes);
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
            Cipher cipher = Cipher.getInstance(ALGORITMO_GESTOPAGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(jsonPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible cifrar la peticion SenTx de GestoPago", e);
        }
    }

    public String descifrarTexto(String signedBase64) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(signedBase64);
            Cipher cipher = Cipher.getInstance(ALGORITMO_GESTOPAGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
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
