package com.t4kash.api.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeHasherTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private final VerificationCodeHasher hasher = new VerificationCodeHasher(SECRET);

    @Test
    void storesNeitherTheCodeNorAPlainHash() {
        String stored = hasher.hash("INICIO_SESION_2FA", "ana@uni.edu.ni", "123456");

        assertThat(stored).startsWith("v1$").doesNotContain("123456");
        assertThat(stored.split("\\$")).hasSize(3);
        assertThat(stored.length()).isLessThanOrEqualTo(128);
    }

    @Test
    void matchesOnlyTheSameCodePurposeAndEmail() {
        String stored = hasher.hash("INICIO_SESION_2FA", "Ana@uni.edu.ni", "123456");

        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni ", "123456", stored)).isTrue();
        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", "123457", stored)).isFalse();
        assertThat(hasher.matches("CORREO_INSTITUCIONAL", "ana@uni.edu.ni", "123456", stored)).isFalse();
        assertThat(hasher.matches("INICIO_SESION_2FA", "otra@uni.edu.ni", "123456", stored)).isFalse();
    }

    @Test
    void theSameCodeProducesDifferentStoredValues() {
        // La sal evita choques en columnas UNIQUE cuando dos codigos coinciden.
        String first = hasher.hash("RECUPERACION", "ana@uni.edu.ni", "123456");
        String second = hasher.hash("RECUPERACION", "ana@uni.edu.ni", "123456");

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches("RECUPERACION", "ana@uni.edu.ni", "123456", second)).isTrue();
    }

    @Test
    void aLeakedValueIsUselessWithoutTheSecret() {
        String stored = hasher.hash("INICIO_SESION_2FA", "ana@uni.edu.ni", "123456");
        VerificationCodeHasher otherServer = new VerificationCodeHasher("fedcba9876543210fedcba9876543210");

        assertThat(otherServer.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", "123456", stored)).isFalse();
    }

    @Test
    void stillAcceptsPlainCodesCreatedBeforeTheChange() {
        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", "654321", "654321")).isTrue();
        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", "654320", "654321")).isFalse();
        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", null, "654321")).isFalse();
        assertThat(hasher.matches("INICIO_SESION_2FA", "ana@uni.edu.ni", "654321", "basura")).isFalse();
    }

    @Test
    void worksWithATemporarySecretWhenNoneIsConfigured() {
        VerificationCodeHasher withoutSecret = new VerificationCodeHasher("");
        String stored = withoutSecret.hash("CORREO_INSTITUCIONAL", "ana@gmail.com", "111111");

        assertThat(withoutSecret.matches("CORREO_INSTITUCIONAL", "ana@gmail.com", "111111", stored)).isTrue();
    }
}
