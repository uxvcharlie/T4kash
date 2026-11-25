package com.t4kash.api.identity.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.t4kash.api.exception.TooManyAttemptsException;
import com.t4kash.api.identity.entity.IntentoLogin;
import com.t4kash.api.identity.repository.IntentoLoginRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LoginSecurityServiceTest {
    @Mock
    private IntentoLoginRepository intentoRepository;

    private static final String EMAIL = "student@example.com";
    private static final String IP = "203.0.113.7";

    @Test
    void allowsLoginWhenNeitherLimitWasReached() {
        LoginSecurityService service = service();
        noRecentSuccess();
        pairFailures(4L);
        accountFailures(4L);

        assertDoesNotThrow(() -> service.requireAvailable(EMAIL, IP));
    }

    @Test
    void blocksTheSameEmailAndIpAfterFiveRecentFailures() {
        LoginSecurityService service = service();
        noRecentSuccess();
        pairFailures(5L);
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndIpOrigenAndExitosoFalseOrderByFechaIntentoDesc(EMAIL, IP))
                .thenReturn(Optional.of(failureMinutesAgo(1)));

        assertThrows(TooManyAttemptsException.class, () -> service.requireAvailable(EMAIL, IP));
    }

    @Test
    void failuresFromAnotherIpDoNotLockTheOwnerOut() {
        // Cinco fallos desde la IP del atacante no cuentan para la IP de la duena.
        LoginSecurityService service = service();
        noRecentSuccess();
        pairFailures(0L);
        accountFailures(5L);

        assertDoesNotThrow(() -> service.requireAvailable(EMAIL, IP));
    }

    @Test
    void blocksTheAccountAfterTwentyFailuresFromAnyIp() {
        LoginSecurityService service = service();
        noRecentSuccess();
        pairFailures(1L);
        accountFailures(20L);
        when(intentoRepository.findFirstByCorreoIgnoreCaseAndExitosoFalseOrderByFechaIntentoDesc(EMAIL))
                .thenReturn(Optional.of(failureMinutesAgo(2)));

        assertThrows(TooManyAttemptsException.class, () -> service.requireAvailable(EMAIL, IP));
    }

    @Test
    void releasesTheLockOnceTheLockTimePassed() {
        LoginSecurityService service = service();
        noRecentSuccess();
        pairFailures(5L);
        accountFailures(5L);
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndIpOrigenAndExitosoFalseOrderByFechaIntentoDesc(EMAIL, IP))
                .thenReturn(Optional.of(failureMinutesAgo(16)));

        assertDoesNotThrow(() -> service.requireAvailable(EMAIL, IP));
    }

    @Test
    void treatsAMissingIpAsUnknownInsteadOfSkippingTheLimit() {
        LoginSecurityService service = service();
        noRecentSuccess();
        when(intentoRepository
                .countByCorreoIgnoreCaseAndIpOrigenAndExitosoFalseAndFechaIntentoAfter(
                        eq(EMAIL), eq(LoginSecurityService.UNKNOWN_IP), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(intentoRepository
                .findFirstByCorreoIgnoreCaseAndIpOrigenAndExitosoFalseOrderByFechaIntentoDesc(
                        EMAIL, LoginSecurityService.UNKNOWN_IP))
                .thenReturn(Optional.of(failureMinutesAgo(1)));

        assertThrows(TooManyAttemptsException.class, () -> service.requireAvailable(EMAIL, null));
    }

    private void noRecentSuccess() {
        when(intentoRepository.findFirstByCorreoIgnoreCaseAndExitosoTrueOrderByFechaIntentoDesc(EMAIL))
                .thenReturn(Optional.empty());
    }

    private void pairFailures(long count) {
        when(intentoRepository
                .countByCorreoIgnoreCaseAndIpOrigenAndExitosoFalseAndFechaIntentoAfter(
                        eq(EMAIL), eq(IP), any(LocalDateTime.class)))
                .thenReturn(count);
    }

    private void accountFailures(long count) {
        when(intentoRepository
                .countByCorreoIgnoreCaseAndExitosoFalseAndFechaIntentoAfter(eq(EMAIL), any(LocalDateTime.class)))
                .thenReturn(count);
    }

    private IntentoLogin failureMinutesAgo(int minutes) {
        IntentoLogin failure = new IntentoLogin();
        failure.setFechaIntento(LocalDateTime.now().minusMinutes(minutes));
        return failure;
    }

    @Test
    void recordsFailureMetadata() {
        LoginSecurityService service = service();

        service.recordFailure(
                7,
                "student@example.com",
                "CREDENCIALES_INVALIDAS",
                "127.0.0.1",
                "test-agent"
        );

        ArgumentCaptor<IntentoLogin> captor = ArgumentCaptor.forClass(
                IntentoLogin.class
        );
        verify(intentoRepository).save(captor.capture());
        IntentoLogin attempt = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(7, attempt.getIdUsuario());
        org.junit.jupiter.api.Assertions.assertEquals(
                "student@example.com",
                attempt.getCorreo()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "CREDENCIALES_INVALIDAS",
                attempt.getMotivoFallo()
        );
        org.junit.jupiter.api.Assertions.assertFalse(attempt.isExitoso());
    }

    private LoginSecurityService service() {
        return new LoginSecurityService(intentoRepository, 5, 20, 15, 15);
    }
}
