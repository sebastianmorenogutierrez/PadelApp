
package com.example.dao;

import com.example.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // Necesitas importar List

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Integer> {

        @Query("SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario AND u.eliminado = false")
        Usuario buscarPorNombre(@Param("nombreUsuario") String nombreUsuario);

        @Query("SELECT u FROM Usuario u JOIN FETCH u.individuo WHERE u.eliminado = false")
        List<Usuario> findByEliminadoFalse();
}