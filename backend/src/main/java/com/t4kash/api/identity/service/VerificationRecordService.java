package com.t4kash.api.identity.service;

import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.VerificacionUsuarioRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Crea registros de VerificacionUsuario para el registro y el inicio de
 * sesion, manteniendo la misma estructura en ambos flujos.
 */
@Service
public class VerificationRecordService {
    private static final String PENDING_VERIFICATION = "PENDIENTE";

    private final VerificacionUsuarioRepository verificacionRepository;
    private final VerificationCodeService codeService;
    private final VerificationCodeHasher codeHasher;

    public VerificationRecordService(
            VerificacionUsuarioRepository verificacionRepository,
            VerificationCodeService codeService,
            VerificationCodeHasher codeHasher
    ) {
        this.verificacionRepository = verificacionRepository;
        this.codeService = codeService;
        this.codeHasher = codeHasher;
    }

    /**
     * Registro guardado y codigo en claro. El codigo solo se usa para enviarlo
     * por correo: en la base queda su HMAC.
     */
    public record IssuedVerification(VerificacionUsuario verification, String code) {
    }

    public IssuedVerification create(
            Usuario usuario,
            LocalDateTime now,
            String verificationType,
            int expirationMinutes
    ) {
        VerificacionUsuario verification = new VerificacionUsuario();
        verification.setIdUsuario(usuario.getIdUsuario());
        verification.setCorreoInstitucional(usuario.getCorreo());
        String code = codeService.generate();
        verification.setCodigoVerificacion(
                codeHasher.hash(verificationType, usuario.getCorreo(), code)
        );
        verification.setEstadoVerificacion(PENDING_VERIFICATION);
        verification.setFechaSolicitud(now);
        verification.setFechaExpiracion(now.plusMinutes(expirationMinutes));
        verification.setTipoVerificacion(verificationType);
        return new IssuedVerification(verificacionRepository.save(verification), code);
    }

    /** Compara el codigo recibido con el guardado, en tiempo constante. */
    public boolean matches(VerificacionUsuario verification, String rawCode) {
        return codeHasher.matches(
                verification.getTipoVerificacion(),
                verification.getCorreoInstitucional(),
                rawCode,
                verification.getCodigoVerificacion()
        );
    }
}
