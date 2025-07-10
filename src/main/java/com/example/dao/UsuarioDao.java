package com.example.dao;

import com.example.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

        @Query("SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario AND u.eliminado = false")
        Usuario buscarPorNombre(@Param("nombreUsuario") String nombreUsuario);

        Usuario findByNombreUsuario(String nombreUsuario);
}
