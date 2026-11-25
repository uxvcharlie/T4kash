package com.t4kash.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita por IP las peticiones POST a {@code /api/auth/**}.
 *
 * <p>Frena la fuerza bruta de claves y codigos y el abuso de las rutas que
 * envian correo, que gastan la cuota diaria del proveedor. Es una ventana
 * fija en memoria: alcanza para una sola instancia (Render gratis). Con
 * varias instancias habria que moverlo a un almacen compartido.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    static final Set<String> EMAIL_SENDING_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/resend-verification",
            "/api/auth/login/resend",
            "/api/auth/password/forgot"
    );
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ClientIpResolver clientIpResolver;
    private final Clock clock;
    private final int authRequestsPerMinute;
    private final int emailRequestsPerWindow;
    private final long emailWindowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(
            ClientIpResolver clientIpResolver,
            @Value("${app.rate-limit.auth-requests-per-minute:20}") int authRequestsPerMinute,
            @Value("${app.rate-limit.email-requests-per-window:5}") int emailRequestsPerWindow,
            @Value("${app.rate-limit.email-window-minutes:10}") int emailWindowMinutes
    ) {
        this(clientIpResolver, Clock.systemUTC(), authRequestsPerMinute, emailRequestsPerWindow, emailWindowMinutes);
    }

    AuthRateLimitFilter(
            ClientIpResolver clientIpResolver,
            Clock clock,
            int authRequestsPerMinute,
            int emailRequestsPerWindow,
            int emailWindowMinutes
    ) {
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.emailRequestsPerWindow = emailRequestsPerWindow;
        this.emailWindowMillis = emailWindowMinutes * 60_000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = clientIpResolver.resolve(request);
        long now = clock.millis();

        long retryAfter = consume("auth|" + ip + "|" + path, authRequestsPerMinute, 60_000L, now);
        if (retryAfter == 0 && EMAIL_SENDING_PATHS.contains(path)) {
            retryAfter = consume("email|" + ip, emailRequestsPerWindow, emailWindowMillis, now);
        }
        if (retryAfter > 0) {
            reject(response, path, retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Devuelve 0 si la peticion entra, o los segundos que faltan para reintentar. */
    private long consume(String key, int limit, long windowMillis, long now) {
        if (limit <= 0) {
            return 0;
        }
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        }
        Window window = windows.compute(key, (ignored, current) ->
                current == null || current.expiresAt <= now
                        ? new Window(now + windowMillis, 1)
                        : new Window(current.expiresAt, current.count + 1)
        );
        if (window.count <= limit) {
            return 0;
        }
        return Math.max(1, (window.expiresAt - now + 999) / 1000);
    }

    private void reject(HttpServletResponse response, String path, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Demasiadas solicitudes. Intenta nuevamente en %d segundo(s).",\
                "instance":"%s"}""".formatted(retryAfterSeconds, path.replace("\\", "").replace("\"", "")));
    }

    private record Window(long expiresAt, int count) {
    }
}
