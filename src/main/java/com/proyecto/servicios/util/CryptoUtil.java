package com.proyecto.servicios.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra/descifra texto sensible (correo, jwt) con AES-256-GCM.
 * La llave se toma de una variable de entorno / application.properties, NUNCA hardcodeada.
 * El IV es aleatorio en cada cifrado y se guarda concatenado al inicio del texto cifrado,
 * por eso el mismo texto nunca produce el mismo resultado dos veces (esto es lo correcto,
 * por eso el correo se busca por su HASH -ver HashUtil-, no comparando el texto cifrado).
 */
@Component
public class CryptoUtil {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_TAG_BITS = 128;
    private static final int TAMANO_IV_BYTES = 12;

    private final SecretKeySpec llave;
    private final SecureRandom random = new SecureRandom();

    public CryptoUtil(@Value("${seguridad.aes.llave}") String llaveBase64) {
        byte[] llaveBytes = Base64.getDecoder().decode(llaveBase64.trim());
        this.llave = new SecretKeySpec(llaveBytes, "AES");
    }

    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible cifrar la informacion", e);
        }
    }

    public String descifrar(String textoCifradoBase64) {
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoBase64);

            byte[] iv = new byte[TAMANO_IV_BYTES];
            byte[] cifrado = new byte[datos.length - TAMANO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANO_IV_BYTES);
            System.arraycopy(datos, TAMANO_IV_BYTES, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] textoPlano = cipher.doFinal(cifrado);

            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible descifrar la informacion", e);
        }
    }
}
