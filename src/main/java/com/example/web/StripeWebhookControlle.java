package com.example.web;

import com.example.dao.PagoRepository;
import com.example.model.Pago;
import com.example.servicio.CorreoServicio; // 💡 Importación del servicio de correo
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private PagoRepository pagoRepository;

    // 💡 INYECCIÓN DEL SERVICIO DE CORREO
    @Autowired
    private CorreoServicio correoServicio;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Stripe.apiKey = stripeApiKey;
        Event event = null;

        try {
            // Verificar la firma del webhook (seguridad)
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Firma inválida
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        }

        // Manejar diferentes tipos de eventos
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;

            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;

            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;

            default:
                System.out.println("Evento no manejado: " + event.getType());
        }

        return ResponseEntity.ok("Webhook recibido");
    }

    /**
     * Maneja cuando se completa una sesión de checkout
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (session == null) return;

        // Asegúrate de que este método exista en PagoRepository.java
        Optional<Pago> pagoOpt = pagoRepository
                .findByStripeCheckoutSessionId(session.getId());

        if (pagoOpt.isPresent()) {
            Pago pago = pagoOpt.get();

            // Actualizar el estado del pago
            if ("paid".equals(session.getPaymentStatus())) {
                pago.setEstado("completado");
                pago.setFechaPago(LocalDateTime.now());
                pago.setStripePaymentIntentId(session.getPaymentIntent());
                pago.setMetodoPago("card");

                pagoRepository.save(pago);

                System.out.println("Pago completado: " + pago.getId_pago());

                // 🚀 LÓGICA AGREGADA: Enviar email de confirmación
                try {
                    correoServicio.enviarConfirmacionPago(pago);
                    System.out.println("Email de confirmación de pago enviado para el pago: " + pago.getId_pago());
                } catch (Exception e) {
                    // Es crucial no dejar que un error de correo detenga el procesamiento del webhook
                    System.err.println("ERROR al enviar email de confirmación para el pago " + pago.getId_pago() + ": " + e.getMessage());
                }

                // - Actualizar estado del partido
                // - Notificar a otros jugadores
            }
        }
    }

    /**
     * Maneja cuando un pago es exitoso
     */
    private void handlePaymentIntentSucceeded(Event event) {
        // Lógica adicional si es necesario
        System.out.println("Payment intent succeeded: " + event.getId());
    }

    /**
     * Maneja cuando un pago falla
     */
    private void handlePaymentIntentFailed(Event event) {
        // Marcar el pago como fallido
        System.out.println("Payment intent failed: " + event.getId());
    }
}