package com.t4kash.api.identity.controller;

import com.t4kash.api.config.ClientIpResolver;
import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.*;
import com.t4kash.api.identity.service.AuthSessionService;
import com.t4kash.api.identity.service.LoginService;
import com.t4kash.api.identity.service.PasswordResetService;
import com.t4kash.api.identity.service.RegistrationService;
import com.t4kash.api.identity.web.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";
    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;
    private final AuthSessionService authSessionService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService,
            PasswordResetService passwordResetService,
            AuthSessionService authSessionService,
            ClientIpResolver clientIpResolver
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.passwordResetService = passwordResetService;
        this.authSessionService = authSessionService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        return registrationService.verifyEmail(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @PostMapping("/resend-verification")
    public RegistrationResponse resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return registrationService.resendVerification(request.correo());
    }

    @PostMapping("/login")
    public LoginChallengeResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return loginService.login(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @PostMapping("/login/verify")
    public AuthResponse verifyLogin(
            @Valid @RequestBody VerifyLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return loginService.verifyLogin(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @PostMapping("/login/resend")
    public LoginChallengeResponse resendLoginVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return loginService.resendLoginVerification(request.correo());
    }

    @PostMapping("/password/forgot")
    public MessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return passwordResetService.requestPasswordReset(request.correo());
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return passwordResetService.resetPassword(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@CurrentUser AuthenticatedUserResponse user) {
        return user;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        authSessionService.logout(bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidCredentialsException("Debes iniciar sesion.");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    /**
     * Registra el token FCM del dispositivo para la cuenta de la sesion. El
     * usuario sale siempre del token de sesion, nunca de un id enviado por el
     * cliente.
     */
    @PutMapping("/me/fcm-token")
    public ResponseEntity<Void> updateMyFcmToken(
            @CurrentUser AuthenticatedUserResponse user,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        authSessionService.actualizarTokenFirebase(user.idUsuario(), request.getToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Ruta anterior, conservada para las apps ya instaladas. Exige sesion y
     * solo acepta el id de la propia cuenta.
     *
     * @deprecated usar {@code PUT /api/auth/me/fcm-token}.
     */
    @Deprecated
    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Long id,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        if (id == null || id.longValue() != user.idUsuario().longValue()) {
            throw new ForbiddenOperationException(
                    "Solo puedes registrar el token de tu propia cuenta."
            );
        }
        authSessionService.actualizarTokenFirebase(user.idUsuario(), request.getToken());
        return ResponseEntity.noContent().build();
    }
}