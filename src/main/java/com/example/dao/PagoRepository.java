package com.example.dao;

import com.example.model.Pago;
import com.example.domain.PadelMatch; // 💡 Necesitas importar PadelMatch
import com.example.domain.usuario.Usuario; // 💡 Necesitas importar Usuario
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Método necesario para el StripeWebhookController
    Optional<Pago> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    // 💡 Método agregado para resolver el error en StripeService.usuarioYaPago
    Optional<Pago> findByUsuarioAndPartidoAndEstado(Usuario usuario, PadelMatch partido, String estado);
}