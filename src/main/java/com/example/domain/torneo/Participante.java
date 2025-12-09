package com.example.domain.torneo;

import com.example.domain.usuario.Usuario; // ¡Importar la clase Usuario!
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "participante") // Añadir @Table para buena práctica
public class Participante implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

    // 🏆 NUEVA RELACIÓN: Usuario que se inscribe
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Constructores (se recomienda añadir uno vacío)
    public Participante() {}


    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Torneo getTorneo() {
        return torneo;
    }

    public void setTorneo(Torneo torneo) {
        this.torneo = torneo;
    }

    // 🏆 Nuevo Getter
    public Usuario getUsuario() {
        return usuario;
    }

    // 🏆 Nuevo Setter (Resuelve el error "setUsuario en rojo")
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}