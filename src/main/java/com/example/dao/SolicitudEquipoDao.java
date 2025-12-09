package com.example.dao;

import com.example.domain.SolicitudEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Añadir para el método DELETE
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudEquipoDao extends JpaRepository<SolicitudEquipo, Long> {

    @Query("SELECT s FROM SolicitudEquipo s WHERE s.jugador2.id_usuario = :idJugador2 AND s.estado = 'PENDIENTE'")
    List<SolicitudEquipo> findSolicitudesPendientesByJugador2(@Param("idJugador2") Integer idJugador2); // ⬅️ Integer

    @Query("SELECT s FROM SolicitudEquipo s WHERE s.jugador1.id_usuario = :idJugador1 AND s.estado = 'PENDIENTE'")
    List<SolicitudEquipo> findSolicitudesPendientesByJugador1(@Param("idJugador1") Integer idJugador1); // ⬅️ Integer

    @Query("SELECT s FROM SolicitudEquipo s WHERE " +
            "((s.jugador1.id_usuario = :idJugador1 AND s.jugador2.id_usuario = :idJugador2) OR " +
            "(s.jugador1.id_usuario = :idJugador2 AND s.jugador2.id_usuario = :idJugador1)) AND " +
            "s.estado = 'PENDIENTE'")
    List<SolicitudEquipo> findSolicitudesPendientesEntreJugadores(
            @Param("idJugador1") Integer idJugador1, // ⬅️ Integer
            @Param("idJugador2") Integer idJugador2); // ⬅️ Integer

    List<SolicitudEquipo> findByEstado(String estado);

    @Query("SELECT s FROM SolicitudEquipo s WHERE s.jugador1.id_usuario = :idUsuario OR s.jugador2.id_usuario = :idUsuario")
    List<SolicitudEquipo> findSolicitudesByUsuario(@Param("idUsuario") Integer idUsuario); // ⬅️ Integer

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SolicitudEquipo s WHERE " +
            "((s.jugador1.id_usuario = :idJugador1 AND s.jugador2.id_usuario = :idJugador2) OR " +
            "(s.jugador1.id_usuario = :idJugador2 AND s.jugador2.id_usuario = :idJugador1)) AND " +
            "s.estado = 'PENDIENTE'")
    boolean existeSolicitudPendienteEntreJugadores(
            @Param("idJugador1") Integer idJugador1, // ⬅️ Integer
            @Param("idJugador2") Integer idJugador2); // ⬅️ Integer

    @Query("SELECT s FROM SolicitudEquipo s WHERE s.estado = 'PENDIENTE' AND s.fechaSolicitud < :fecha")
    List<SolicitudEquipo> findSolicitudesPendientesAnterioresA(@Param("fecha") LocalDateTime fecha);

    @Query("SELECT COUNT(s) FROM SolicitudEquipo s WHERE s.jugador2.id_usuario = :idJugador2 AND s.estado = 'PENDIENTE'")
    long contarSolicitudesPendientesPorJugador2(@Param("idJugador2") Integer idJugador2); // ⬅️ Integer

    List<SolicitudEquipo> findAllByOrderByFechaSolicitudDesc();

    // Nota: Los métodos DELETE/UPDATE necesitan @Modifying
    @Modifying
    @Query("DELETE FROM SolicitudEquipo s WHERE " +
            "(s.jugador1.id_usuario = :idJugador1 AND s.jugador2.id_usuario = :idJugador2) OR " +
            "(s.jugador1.id_usuario = :idJugador2 AND s.jugador2.id_usuario = :idJugador1)")
    void deleteSolicitudesEntreJugadores(@Param("idJugador1") Integer idJugador1, @Param("idJugador2") Integer idJugador2); // ⬅️ Integer
}