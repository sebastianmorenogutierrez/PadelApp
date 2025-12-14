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
@Table(name = "equipos")
public class Equipo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Long idEquipo;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombreEquipo;

    @ManyToOne(fetch = FetchType.EAGER)
    // Mantengo 'id_jugador1' asumiendo que es el nombre de columna correcto en tu tabla 'equipos'
    @JoinColumn(name = "id_jugador1", nullable = false)
    private Usuario jugador1;

    @ManyToOne(fetch = FetchType.EAGER)
    // Mantengo 'id_jugador2' asumiendo que es el nombre de columna correcto en tu tabla 'equipos'
    @JoinColumn(name = "id_jugador2", nullable = false)
    private Usuario jugador2;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column
    private LocalDateTime fechaDisolucion;

    @Column(length = 200)
    private String descripcion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "ACTIVO";
        }
    }

    public Equipo() {
    }

    public Equipo(String nombreEquipo, Usuario jugador1, Usuario jugador2) {
        this.nombreEquipo = nombreEquipo;
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.estado = "ACTIVO";
        this.fechaCreacion = LocalDateTime.now();
    }
    public boolean esMiembro(Integer idUsuario) {
        return (jugador1 != null && jugador1.getId_usuario() != null && jugador1.getId_usuario().equals(idUsuario)) ||
                (jugador2 != null && jugador2.getId_usuario() != null && jugador2.getId_usuario().equals(idUsuario));
    }
    public Usuario obtenerCompanero(Integer idUsuario) {
        if (jugador1 != null && jugador1.getId_usuario() != null && jugador1.getId_usuario().equals(idUsuario)) {
            return jugador2;
        } else if (jugador2 != null && jugador2.getId_usuario() != null && jugador2.getId_usuario().equals(idUsuario)) {
            return jugador1;
        }
        return null;
    }

    public boolean estaActivo() {
        return "ACTIVO".equals(estado);
    }
}