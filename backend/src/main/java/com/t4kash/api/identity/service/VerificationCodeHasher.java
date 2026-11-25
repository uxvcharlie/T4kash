package com.t4kash.api.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Protege los codigos de 6 digitos (registro, segundo factor y recuperacion)
 * antes de guardarlos.
 *
 * <p>Un SHA-256 simple no sirve: con la base filtrada se prueban el millon de
 * codigos posibles en segundos. Por eso se usa HMAC-SHA256 con un secreto que
 * no esta en la base ({@code APP_AUTH_CODE_SECRET}) y una sal aleatoria por
 * codigo, que ademas evita valores repetidos en columnas UNIQUE. El valor
 * guardado tiene la forma {@code v1$<sal>$<hmac>}.
 */
@Component
public class VerificationCodeHasher {
    /** Proposito del codigo de recuperacion; los demas usan su tipo de verificacion. */
    public static final String PASSWORD_RESET = "RECUPERACION";

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCodeHasher.class);
    private static final String VERSION = "v1";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final Pattern LEGACY_PLAIN_CODE = Pattern.compile("\\d{6}");

    private final byte[] secret;
    private final SecureRandom random = new SecureRandom();

    public VerificationCodeHasher(@Value("${app.auth.code-secret:}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.length() < MIN_SECRET_LENGTH) {
            // Sin secreto configurado (desarrollo local) se usa uno temporal: los
            // codigos pendientes dejan de valer al reiniciar, nada mas.
            LOGGER.warn(
                    "APP_AUTH_CODE_SECRET no esta configurado o tiene menos de {} caracteres; "
                            + "se usa un secreto temporal y los codigos pendientes no sobreviven a un reinicio.",
                    MIN_SECRET_LENGTH
            );
            byte[] temporary = new byte[32];
            random.nextBytes(temporary);
            this.secret = temporary;
        } else {
            this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String hash(String purpose, String email, String code) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        return VERSION + "$" + saltHex + "$" + hmac(purpose, email, code, saltHex);
    }

    /**
     * Compara en tiempo constante. Acepta tambien los codigos guardados en
     * texto plano antes de este cambio (6 digitos), para no invalidar los que
     * estaban pendientes en el momento del deploy; vencen en minutos.
     */
    public boolean matches(String purpose, String email, String rawCode, String stored) {
        if (rawCode == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length == 3 && VERSION.equals(parts[0])) {
            return constantTimeEquals(hmac(purpose, email, rawCode, parts[1]), parts[2]);
        }
        if (LEGACY_PLAIN_CODE.matcher(stored).matches()) {
            return constantTimeEquals(rawCode, stored);
        }
        return false;
    }

    private String hmac(String purpose, String email, String code, String saltHex) {
        String message = purpose + "|" + normalize(email) + "|" + saltHex + "|" + code;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 no esta disponible.", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
