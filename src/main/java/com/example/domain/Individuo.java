package com.example.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "individuo")
public class Individuo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_individuo;

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotEmpty(message = "El apellido no puede estar vacío")
    private String apellido;

    @NotEmpty(message = "El teléfono no puede estar vacío")
    private String telefono;

    @NotEmpty(message = "El correo no puede estar vacío")
    @Email(message = "Debe ser un correo válido")
    private String correo;

    @NotEmpty(message = "La edad no puede estar vacía")
    private String edad;

    @NotEmpty(message = "La cédula no puede estar vacía")
    private String cedula;

    private boolean eliminado = false;

    public Long getId_individuo() {
        return id_individuo;
    }

    public void setId_individuo(Long id_individuo) {
        this.id_individuo = id_individuo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getEdad() {
        return edad;
    }

    public void setEdad(String edad) {
        this.edad = edad;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}
