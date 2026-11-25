package com.t4kash.api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Obtiene la IP real del cliente para auditoria y limites de intentos.
 *
 * <p>El cliente puede escribir lo que quiera en {@code X-Forwarded-For}; solo
 * son confiables los valores que agregan los proxies propios, que siempre se
 * anaden al final. Con {@code app.http.trusted-proxy-hops = N} se toma el
 * N-esimo valor contando desde la derecha. Con 0 (por defecto, desarrollo
 * local) se ignora el encabezado y se usa la direccion de la conexion.
 */
@Component
public class ClientIpResolver {
    private static final Pattern IP_CHARACTERS = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    private final int trustedProxyHops;

    public ClientIpResolver(@Value("${app.http.trusted-proxy-hops:0}") int trustedProxyHops) {
        this.trustedProxyHops = Math.max(0, trustedProxyHops);
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (trustedProxyHops == 0) {
            return remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remote;
        }
        List<String> hops = Arrays.stream(forwarded.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        if (hops.isEmpty()) {
            return remote;
        }
        int index = Math.max(0, hops.size() - trustedProxyHops);
        String candidate = hops.get(index);
        return IP_CHARACTERS.matcher(candidate).matches() ? candidate : remote;
    }
}
