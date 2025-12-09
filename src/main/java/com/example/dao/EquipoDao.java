package com.example.dao;

import com.example.domain.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoDao extends JpaRepository<Equipo, Long> {

    @Query("SELECT e FROM Equipo e WHERE e.jugador1.id_usuario = :idUsuario OR e.jugador2.id_usuario = :idUsuario")
    List<Equipo> findByUsuario(@Param("idUsuario") Integer idUsuario); // ⬅️ CORREGIDO a Integer

    @Query("SELECT e FROM Equipo e WHERE (e.jugador1.id_usuario = :idUsuario OR e.jugador2.id_usuario = :idUsuario) AND e.estado = 'ACTIVO'")
    List<Equipo> findEquiposActivosByUsuario(@Param("idUsuario") Integer idUsuario); // ⬅️ CORREGIDO a Integer

    List<Equipo> findByEstado(String estado);
    @Query("SELECT e FROM Equipo e WHERE " +
            "(e.jugador1.id_usuario = :idJugador1 AND e.jugador2.id_usuario = :idJugador2) OR " +
            "(e.jugador1.id_usuario = :idJugador2 AND e.jugador2.id_usuario = :idJugador1)")
    Optional<Equipo> findByJugadores(@Param("idJugador1") Integer idJugador1, @Param("idJugador2") Integer idJugador2); // ⬅️ CORREGIDO a Integer

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Equipo e WHERE " +
            "((e.jugador1.id_usuario = :idJugador1 AND e.jugador2.id_usuario = :idJugador2) OR " +
            "(e.jugador1.id_usuario = :idJugador2 AND e.jugador2.id_usuario = :idJugador1)) AND " +
            "e.estado = 'ACTIVO'")
    boolean existeEquipoActivoEntreJugadores(@Param("idJugador1") Integer idJugador1, @Param("idJugador2") Integer idJugador2); // ⬅️ CORREGIDO a Integer

    List<Equipo> findByNombreEquipoContainingIgnoreCase(String nombreEquipo);

    List<Equipo> findByEstadoOrderByFechaCreacionDesc(String estado);

    @Query("SELECT COUNT(e) FROM Equipo e WHERE " +
            "(e.jugador1.id_usuario = :idUsuario OR e.jugador2.id_usuario = :idUsuario) AND " +
            "e.estado = 'ACTIVO'")
    long contarEquiposActivosPorUsuario(@Param("idUsuario") Integer idUsuario); // ⬅️ CORREGIDO a Integer

    @Query("SELECT e FROM Equipo e WHERE e.jugador1.id_usuario = :idUsuario")
    List<Equipo> findEquiposCreadosPorUsuario(@Param("idUsuario") Integer idUsuario); // ⬅️ CORREGIDO a Integer
}