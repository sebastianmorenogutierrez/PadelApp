package com.example.domain;

import com.example.domain.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitudes_equipo")
public class SolicitudEquipo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_jugador1", nullable = false)
    private Usuario jugador1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_jugador2", nullable = false)
    private Usuario jugador2;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombreEquipo;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column
    private LocalDateTime fechaRespuesta;

    @Column(length = 200)
    private String mensaje;

    @PrePersist
    protected void onCreate() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    public SolicitudEquipo() {
    }

    public SolicitudEquipo(Usuario jugador1, Usuario jugador2, String nombreEquipo) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.nombreEquipo = nombreEquipo;
        this.estado = "PENDIENTE";
        this.fechaSolicitud = LocalDateTime.now();
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equals(estado);
    }

    public void aceptar() {
        this.estado = "ACEPTADA";
        this.fechaRespuesta = LocalDateTime.now();
    }

    public void rechazar() {
        this.estado = "RECHAZADA";
        this.fechaRespuesta = LocalDateTime.now();
    }

    public void cancelar() {
        this.estado = "CANCELADA";
        this.fechaRespuesta = LocalDateTime.now();
    }

    public boolean haExpirado() {
        if (fechaSolicitud == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(fechaSolicitud.plusDays(30));
    }
}