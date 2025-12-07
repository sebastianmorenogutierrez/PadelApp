package com.example.servicio;

import com.example.dao.PagoRepository; // 💡 Importación necesaria
import com.example.domain.PadelMatch;   // 💡 Importación necesaria
import com.example.domain.usuario.Usuario; // 💡 Importación necesaria
import com.example.model.Pago;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired; // 💡 Importación necesaria
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional; // 💡 Importación necesaria

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    // 💡 INYECCIÓN DEL REPOSITORIO DE PAGO
    @Autowired
    private PagoRepository pagoRepository;

    public Session crearSesionPago(Long pagoId, Long montoCentavos) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(montoCentavos) // monto en centavos
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Pago Partido " + pagoId)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }

    /**
     * 💡 NUEVO MÉTODO: Verifica si el usuario ya realizó un pago completado para este partido.
     * Es crucial para que el controlador pueda validar antes de crear una nueva sesión de pago.
     */
    public boolean usuarioYaPago(Usuario usuario, PadelMatch partido) {
        // Llama al método que debe estar definido en PagoRepository
        Optional<Pago> pagoExistente = pagoRepository
                .findByUsuarioAndPartidoAndEstado(usuario, partido, "completado");

        return pagoExistente.isPresent();
    }
}