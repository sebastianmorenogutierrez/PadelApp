package com.example.model;

import com.example.domain.PadelMatch;
import com.example.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "pago")
public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_pago;
    @ManyToOne
    @JoinColumn(name = "match_id", referencedColumnName = "id_match")
    private PadelMatch partido;

    @ManyToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda = "COP"; // Peso colombiano

    @Column(nullable = false)
    private String estado; // pendiente, completado, fallido, reembolsado

    @Column(unique = true)
    private String stripePaymentIntentId; // ID de Stripe

    // ✅ ESTA ES LA ÚNICA LÍNEA CORRECTA QUE DEBE QUEDAR
    @Column(unique = true)
    private String stripeCheckoutSessionId; // ID de sesión de Stripe

    private LocalDateTime fechaPago;

    private LocalDateTime fechaCreacion;


    private String descripcion;

    private String metodoPago; // card, transfer, etc.

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = "pendiente";
        }
    }
}