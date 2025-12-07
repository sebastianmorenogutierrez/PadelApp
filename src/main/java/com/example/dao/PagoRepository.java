package com.example.dao;

import com.example.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // 💡 ¡Necesitas importar Optional!

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}