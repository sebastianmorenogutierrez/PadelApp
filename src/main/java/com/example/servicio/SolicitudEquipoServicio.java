package com.example.servicio;

import com.example.dao.SolicitudEquipoDao;
import com.example.dao.EquipoDao; // Necesario para crear el equipo al aceptar la solicitud
import com.example.domain.Equipo;
import com.example.domain.SolicitudEquipo;
import com.example.domain.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SolicitudEquipoServicio {

    @Autowired
    private SolicitudEquipoDao solicitudEquipoDao;

    @Autowired
    private EquipoDao equipoDao; // Inyectamos el DAO de Equipo para la creación

    @Autowired
    private EquipoServicio equipoServicio; // Para usar la lógica de verificación de equipo activo

    // --- Métodos de Lectura (Transactional ReadOnly) ---

    @Transactional(readOnly = true)
    public List<SolicitudEquipo> obtenerTodasLasSolicitudes() {
        return solicitudEquipoDao.findAllByOrderByFechaSolicitudDesc();
    }

    @Transactional(readOnly = true)
    public SolicitudEquipo obtenerSolicitudPorId(Long idSolicitud) {
        return solicitudEquipoDao.findById(idSolicitud).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SolicitudEquipo> obtenerSolicitudesPorUsuario(Integer idUsuario) {
        return solicitudEquipoDao.findSolicitudesByUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<SolicitudEquipo> obtenerSolicitudesPendientesRecibidas(Integer idJugador2) {
        return solicitudEquipoDao.findSolicitudesPendientesByJugador2(idJugador2);
    }

    @Transactional(readOnly = true)
    public List<SolicitudEquipo> obtenerSolicitudesPendientesEnviadas(Integer idJugador1) {
        return solicitudEquipoDao.findSolicitudesPendientesByJugador1(idJugador1);
    }

    @Transactional(readOnly = true)
    public boolean existeSolicitudPendienteEntreJugadores(Integer idJugador1, Integer idJugador2) {
        return solicitudEquipoDao.existeSolicitudPendienteEntreJugadores(idJugador1, idJugador2);
    }

    // --- Métodos de Escritura (Transactional) ---

    @Transactional
    public SolicitudEquipo enviarSolicitud(SolicitudEquipo solicitud) {
        if (solicitud == null || solicitud.getJugador1() == null || solicitud.getJugador2() == null) {
            throw new IllegalArgumentException("La solicitud, jugador 1 o jugador 2 no pueden ser nulos.");
        }

        Integer idJugador1 = solicitud.getJugador1().getId_usuario();
        Integer idJugador2 = solicitud.getJugador2().getId_usuario();

        if (idJugador1.equals(idJugador2)) {
            throw new IllegalArgumentException("Un jugador no puede solicitar unirse consigo mismo.");
        }

        // 1. Verificar si ya existe una solicitud PENDIENTE entre ellos (en cualquier dirección)
        boolean existeSolicitud = solicitudEquipoDao.existeSolicitudPendienteEntreJugadores(idJugador1, idJugador2);
        if (existeSolicitud) {
            throw new IllegalStateException("Ya existe una solicitud pendiente entre estos jugadores.");
        }

        // 2. Verificar si ya existe un equipo ACTIVO entre ellos
        // ⬅️ CORRECCIÓN: Quitamos .longValue() para pasar Integer a EquipoServicio
        boolean existeEquipoActivo = equipoServicio.existeEquipoActivoEntreJugadores(idJugador1, idJugador2);
        if (existeEquipoActivo) {
            throw new IllegalStateException("Ya existe un equipo activo entre estos jugadores.");
        }

        // 3. Establecer estados y fechas iniciales (aunque @PrePersist ya lo hace)
        solicitud.setEstado("PENDIENTE");
        solicitud.setFechaSolicitud(LocalDateTime.now());

        return solicitudEquipoDao.save(solicitud);
    }

    @Transactional
    public Equipo aceptarSolicitud(Long idSolicitud) {
        SolicitudEquipo solicitud = solicitudEquipoDao.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("Solo se pueden aceptar solicitudes pendientes.");
        }

        Usuario jugador1 = solicitud.getJugador1();
        Usuario jugador2 = solicitud.getJugador2();

        // 1. Verificar nuevamente que no exista equipo activo
        // ⬅️ CORRECCIÓN: Quitamos .longValue() para pasar Integer a EquipoServicio
        boolean existeEquipoActivo = equipoServicio.existeEquipoActivoEntreJugadores(
                jugador1.getId_usuario(),
                jugador2.getId_usuario()
        );

        if (existeEquipoActivo) {
            // Si ya existe un equipo, rechazamos la solicitud
            solicitud.rechazar();
            solicitud.setMensaje("Rechazada automáticamente: Ya se creó un equipo activo previamente.");
            solicitudEquipoDao.save(solicitud);
            throw new IllegalStateException("No se puede aceptar la solicitud, ya existe un equipo activo entre los jugadores.");
        }

        // 2. Marcar la solicitud como ACEPTADA
        solicitud.aceptar();
        solicitudEquipoDao.save(solicitud);

        // 3. Crear el nuevo equipo
        // ⬅️ CORRECCIÓN: Se usa el constructor de 3 parámetros. El estado "ACTIVO" se establece dentro del constructor de Equipo.
        Equipo nuevoEquipo = new Equipo(solicitud.getNombreEquipo(), jugador1, jugador2);

        // Ya que el constructor de 3 parámetros ya establece fechaCreacion, esta línea es redundante,
        // pero la dejamos comentada por si la quieres eliminar:
        // nuevoEquipo.setFechaCreacion(LocalDateTime.now());

        // Asumimos que EquipoServicio.guardarEquipo maneja la persistencia y validaciones finales.
        return equipoServicio.guardarEquipo(nuevoEquipo);
    }

    @Transactional
    public SolicitudEquipo rechazarSolicitud(Long idSolicitud, String mensaje) {
        SolicitudEquipo solicitud = solicitudEquipoDao.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes pendientes.");
        }

        solicitud.rechazar();
        solicitud.setMensaje(mensaje);
        return solicitudEquipoDao.save(solicitud);
    }

    @Transactional
    public SolicitudEquipo cancelarSolicitud(Long idSolicitud) {
        SolicitudEquipo solicitud = solicitudEquipoDao.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("Solo se pueden cancelar solicitudes pendientes.");
        }

        solicitud.cancelar();
        return solicitudEquipoDao.save(solicitud);
    }

    @Transactional
    public void eliminarSolicitud(Long idSolicitud) {
        if (!solicitudEquipoDao.existsById(idSolicitud)) {
            throw new IllegalArgumentException("Solicitud no encontrada.");
        }
        solicitudEquipoDao.deleteById(idSolicitud);
    }

    @Transactional
    public int limpiarSolicitudesExpiradas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
        List<SolicitudEquipo> expiradas = solicitudEquipoDao.findSolicitudesPendientesAnterioresA(fechaLimite);

        for (SolicitudEquipo solicitud : expiradas) {
            solicitud.setEstado("EXPIRADA");
            solicitud.setFechaRespuesta(LocalDateTime.now());
            solicitudEquipoDao.save(solicitud);
        }
        return expiradas.size();
    }
}