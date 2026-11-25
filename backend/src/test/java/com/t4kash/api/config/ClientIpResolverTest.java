package com.t4kash.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void ignoresForwardedHeaderWhenThereIsNoTrustedProxy() {
        MockHttpServletRequest request = request("10.0.0.5", "1.2.3.4");

        assertThat(new ClientIpResolver(0).resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void takesTheValueAddedByTheTrustedProxyAndNotTheOneSentByTheClient() {
        // El cliente envia "1.2.3.4"; el proxy agrega la IP real al final.
        MockHttpServletRequest request = request("10.0.0.5", "1.2.3.4, 198.51.100.20");

        assertThat(new ClientIpResolver(1).resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void countsSeveralTrustedProxiesFromTheRight() {
        MockHttpServletRequest request = request("10.0.0.5", "1.2.3.4, 198.51.100.20, 172.16.0.9");

        assertThat(new ClientIpResolver(2).resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void fallsBackToTheConnectionWhenTheHeaderIsMissingOrInvalid() {
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.5", null))).isEqualTo("10.0.0.5");
        assertThat(new ClientIpResolver(1).resolve(request("10.0.0.5", "<script>")))
                .isEqualTo("10.0.0.5");
    }

    @Test
    void usesTheLeftmostValueWhenThereAreFewerHopsThanConfigured() {
        MockHttpServletRequest request = request("10.0.0.5", "198.51.100.20");

        assertThat(new ClientIpResolver(3).resolve(request)).isEqualTo("198.51.100.20");
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
