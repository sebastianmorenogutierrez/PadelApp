package com.example.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Usuario findByNombreUsuario(String nombreUsuario);

    @Query("SELECT u FROM Usuario u WHERE u.perfil.descripcion_perfil = :descripcion AND u.eliminado = false")
    List<Usuario> buscarPorPerfil(@Param("descripcion") String descripcion);

}