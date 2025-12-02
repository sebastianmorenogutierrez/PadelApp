package com.example.domain; // Usamos el paquete base para PadelMatch

import com.example.domain.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data; // Asumo que usas Lombok

@Data // Si usas Lombok, simplifica Getters/Setters
@Entity
@Table(name = "padel_match")
public class PadelMatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_match; // Usamos Long para seguir la convención del Torneo

    @NotBlank(message = "El nombre del partido es obligatorio")
    private String nombrePartido;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotBlank(message = "El club es obligatorio")
    private String club;

    // ⭐️ CAMBIO AÑADIDO: Número máximo de jugadores para el PDF y la lógica de inscripción.
    @NotNull(message = "El número máximo de jugadores es obligatorio")
    private Integer numeroJugadores = 4; // Por defecto 4 para un partido de dobles

    private String nivelJuego;
    private String cancha;
    private boolean activo = true;
    private String estado = "programado"; // Opciones: programado, en curso, finalizado
    private LocalDateTime fechaCreacion;

    // Usuario que crea/es dueño del partido (ManyToOne)
    @ManyToOne
    @JoinColumn(name = "creador_id", referencedColumnName = "id_usuario")
    private Usuario creador;

    // Relación Many-to-Many: Jugadores en el partido
    @ManyToMany(fetch = FetchType.EAGER) // Eager fetch para listar jugadores fácilmente
    @JoinTable(
            name = "match_jugadores",
            joinColumns = @JoinColumn(name = "match_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> jugadores = new ArrayList<>();

    // Constructores, Getters/Setters (generados por @Data de Lombok)

    public PadelMatch() {
        this.fechaCreacion = LocalDateTime.now();
    }
}