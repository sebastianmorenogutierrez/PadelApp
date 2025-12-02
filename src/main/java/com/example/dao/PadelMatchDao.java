package com.example.dao;

import com.example.domain.PadelMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PadelMatchDao extends JpaRepository<PadelMatch, Long> {

    List<PadelMatch> findByNombrePartidoContainingIgnoreCase(String nombrePartido);

    List<PadelMatch> findByActivoTrue();

    // Consulta para partidos próximos (fecha en el futuro)
    @Query("SELECT pm FROM PadelMatch pm WHERE pm.fecha >= :fechaActual ORDER BY pm.fecha ASC, pm.hora ASC")
    List<PadelMatch> findMatchesProximos(@Param("fechaActual") LocalDate fechaActual);

    // Consulta para partidos en curso (hoy)
    @Query("SELECT pm FROM PadelMatch pm WHERE pm.fecha = :fechaActual")
    List<PadelMatch> findMatchesDeHoy(@Param("fechaActual") LocalDate fechaActual);

    // Consulta para partidos finalizados (fecha en el pasado)
    @Query("SELECT pm FROM PadelMatch pm WHERE pm.fecha < :fechaActual ORDER BY pm.fecha DESC")
    List<PadelMatch> findMatchesFinalizados(@Param("fechaActual") LocalDate fechaActual);
}