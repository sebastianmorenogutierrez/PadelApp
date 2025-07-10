package com.example.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "padel_matches") // This maps the entity to a table named 'padel_matches' in your DB
public class PadelMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for each match

    private String nombrePartido;
    private LocalDate fecha;
    private LocalTime hora;
    private String club;
    private String nivelJuego;
    private String cancha;

    // Default constructor (required by JPA)
    public PadelMatch() {
    }

    // Constructor with all fields (optional, but good for testing)
    public PadelMatch(String nombrePartido, LocalDate fecha, LocalTime hora, String club, String nivelJuego, String cancha) {
        this.nombrePartido = nombrePartido;
        this.fecha = fecha;
        this.hora = hora;
        this.club = club;
        this.nivelJuego = nivelJuego;
        this.cancha = cancha;
    }

    // Getters and Setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombrePartido() {
        return nombrePartido;
    }

    public void setNombrePartido(String nombrePartido) {
        this.nombrePartido = nombrePartido;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getNivelJuego() {
        return nivelJuego;
    }

    public void setNivelJuego(String nivelJuego) {
        this.nivelJuego = nivelJuego;
    }

    public String getCancha() {
        return cancha;
    }

    public void setCancha(String cancha) {
        this.cancha = cancha;
    }

    @Override
    public String toString() {
        return "PadelMatch{" +
                "id=" + id +
                ", nombrePartido='" + nombrePartido + '\'' +
                ", fecha=" + fecha +
                ", hora=" + hora +
                ", club='" + club + '\'' +
                ", nivelJuego='" + nivelJuego + '\'' +
                ", cancha='" + cancha + '\'' +
                '}';
    }
}