package com.t4kash.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    private final AtomicLong millis = new AtomicLong(1_000_000L);
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }
    };

    @Test
    void rejectsLoginAttemptsAboveTheLimitPerMinute() throws Exception {
        AuthRateLimitFilter filter = filter(3, 100);

        for (int i = 0; i < 3; i++) {
            assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = post(filter, "/api/auth/login", "203.0.113.7");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("Demasiadas solicitudes");
    }

    @Test
    void countsEachIpSeparately() throws Exception {
        AuthRateLimitFilter filter = filter(1, 100);

        assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(429);
        assertThat(post(filter, "/api/auth/login", "198.51.100.20").getStatus()).isEqualTo(200);
    }

    @Test
    void sharesOneStricterBudgetAcrossTheRoutesThatSendEmail() throws Exception {
        AuthRateLimitFilter filter = filter(100, 2);

        assertThat(post(filter, "/api/auth/register", "203.0.113.7").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/auth/password/forgot", "203.0.113.7").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/auth/login/resend", "203.0.113.7").getStatus()).isEqualTo(429);
        // Verificar un codigo no envia correo: no gasta ese presupuesto.
        assertThat(post(filter, "/api/auth/verify-email", "203.0.113.7").getStatus()).isEqualTo(200);
    }

    @Test
    void opensAgainWhenTheWindowEnds() throws Exception {
        AuthRateLimitFilter filter = filter(1, 100);

        assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(429);
        millis.addAndGet(60_000L);
        assertThat(post(filter, "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
    }

    @Test
    void leavesOtherRoutesAndMethodsAlone() throws Exception {
        AuthRateLimitFilter filter = filter(1, 1);

        for (int i = 0; i < 5; i++) {
            assertThat(post(filter, "/api/tasks", "203.0.113.7").getStatus()).isEqualTo(200);
            MockHttpServletRequest me = new MockHttpServletRequest("GET", "/api/auth/me");
            me.setRemoteAddr("203.0.113.7");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(me, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private AuthRateLimitFilter filter(int authPerMinute, int emailPerWindow) {
        return new AuthRateLimitFilter(new ClientIpResolver(0), clock, authPerMinute, emailPerWindow, 10);
    }

    private MockHttpServletResponse post(AuthRateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
