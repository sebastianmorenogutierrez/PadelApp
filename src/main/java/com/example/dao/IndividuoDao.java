package com.example.dao;

import com.example.domain.Individuo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IndividuoDao extends JpaRepository<Individuo, Long> {

    @Query("SELECT i FROM Individuo i WHERE i.nombre = :nombre AND i.eliminado = false")
    Individuo buscarPorNombre(@Param("nombre") String nombre);

}
