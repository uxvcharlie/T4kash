package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCaseAndIdUsuarioNot(
            String nombreUsuario,
            Integer idUsuario
    );

    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndEstadoUsuarioIgnoreCase(
            String nombreUsuario,
            String estadoUsuario
    );

    /**
     * Un token FCM identifica un dispositivo: si otra cuenta lo tenia (el
     * telefono cambio de sesion), deja de recibir sus notificaciones.
     */
    @Modifying
    @Query("""
            UPDATE Usuario usuario
            SET usuario.fcmToken = NULL
            WHERE usuario.fcmToken = :token
              AND usuario.idUsuario <> :idUsuario
            """)
    int releaseFcmTokenFromOtherUsers(
            @Param("token") String token,
            @Param("idUsuario") Integer idUsuario
    );
}
