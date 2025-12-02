package com.example.dao;

import com.example.domain.torneo.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;

public interface TorneoDao extends JpaRepository<Torneo, Long> {

    List<Torneo> findByNombreContainingIgnoreCase(String nombre);

    List<Torneo> findByActivoTrue();

    List<Torneo> findByActivoFalse();

    @Query("SELECT t FROM Torneo t WHERE t.fechaInicio >= :fechaActual")
    List<Torneo> findTorneosProximos(@Param("fechaActual") Date fechaActual);

    @Query("SELECT t FROM Torneo t WHERE t.fechaInicio <= :fechaActual AND t.fechaFin >= :fechaActual")
    List<Torneo> findTorneosEnCurso(@Param("fechaActual") Date fechaActual);

    @Query("SELECT t FROM Torneo t WHERE t.fechaFin < :fechaActual")
    List<Torneo> findTorneosFinalizados(@Param("fechaActual") Date fechaActual);
}