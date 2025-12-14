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
// El nombre de la tabla debe coincidir con el SQL
@Table(name = "solicitud_equipo")
public class SolicitudEquipo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Mapear el camelCase 'idSolicitud' al snake_case 'id_solicitud' de la DB
    @Column(name = "id_solicitud")
    private Long idSolicitud;
    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    // La columna de clave foránea se llama 'jugador_invitador_id'
    @JoinColumn(name = "jugador_invitador_id", nullable = false)
    private Usuario jugador1; // Jugador que envía la invitación (Jugador Invitador)

    @ManyToOne(fetch = FetchType.EAGER)
    // La columna de clave foránea se llama 'jugador_invitado_id'
    @JoinColumn(name = "jugador_invitado_id", nullable = false)
    private Usuario jugador2; // Jugador que recibe la invitación (Jugador Invitado)

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombreEquipo;

    // Columna de estado. Se recomienda mapear explícitamente.
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    // Mapeo a fecha_creacion en la DB (Tu SQL usó fecha_creacion)
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column
    private LocalDateTime fechaRespuesta; // Este campo no estaba en tu SQL, si lo necesitas, debes agregarlo a la tabla

    @Column(length = 200)
    private String mensaje; // Este campo no estaba en tu SQL, si lo necesitas, debes agregarlo a la tabla

    @PrePersist
    protected void onCreate() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    // Los constructores, getters y setters (generados por @Data de Lombok) y métodos de negocio se mantienen
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