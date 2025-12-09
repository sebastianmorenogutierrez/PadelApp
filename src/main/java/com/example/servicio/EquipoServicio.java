package com.example.servicio;

import com.example.dao.EquipoDao;
import com.example.domain.Equipo;
import com.example.domain.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EquipoServicio {

    @Autowired
    private EquipoDao equipoDao;

    // --- Métodos de Lectura (Transactional ReadOnly) ---

    @Transactional(readOnly = true)
    public List<Equipo> obtenerTodosLosEquipos() {
        return equipoDao.findAll();
    }

    @Transactional(readOnly = true)
    public Equipo obtenerEquipoPorId(Long idEquipo) {
        return equipoDao.findById(idEquipo).orElse(null);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public List<Equipo> obtenerEquiposPorUsuario(Integer idUsuario) {
        return equipoDao.findByUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public List<Equipo> obtenerEquiposActivosPorUsuario(Integer idUsuario) {
        return equipoDao.findEquiposActivosByUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<Equipo> obtenerEquiposPorEstado(String estado) {
        return equipoDao.findByEstado(estado);
    }

    // --- Métodos de Escritura (Transactional) ---

    @Transactional
    public Equipo guardarEquipo(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo no puede ser nulo");
        }

        if (equipo.getJugador1() == null || equipo.getJugador2() == null) {
            throw new IllegalArgumentException("Ambos jugadores deben estar definidos");
        }

        // getId_usuario() devuelve Integer. El DAO espera Integer.
        if (equipo.getJugador1().getId_usuario().equals(equipo.getJugador2().getId_usuario())) {
            throw new IllegalArgumentException("Los dos jugadores no pueden ser la misma persona");
        }

        if (equipo.getIdEquipo() == null) {
            // El DAO espera Integer
            boolean existeEquipo = equipoDao.existeEquipoActivoEntreJugadores(
                    equipo.getJugador1().getId_usuario(),
                    equipo.getJugador2().getId_usuario()
            );

            if (existeEquipo) {
                throw new IllegalStateException("Ya existe un equipo activo entre estos jugadores");
            }

            // Nota: Asumiendo que la entidad Equipo maneja la fechaCreacion en su constructor o @PrePersist.
            // Si no es así, descomenta: equipo.setFechaCreacion(LocalDateTime.now());
        }

        return equipoDao.save(equipo);
    }

    // ... dentro de EquipoServicio.java

    @Transactional
    public Equipo crearEquipo(String nombreEquipo, Usuario jugador1, Usuario jugador2) {
        // El DAO espera Integer
        boolean existeEquipo = equipoDao.existeEquipoActivoEntreJugadores(
                jugador1.getId_usuario(),
                jugador2.getId_usuario()
        );

        if (existeEquipo) {
            throw new IllegalStateException("Ya existe un equipo activo entre estos jugadores");
        }

        // ⬅️ CORRECCIÓN: Se elimina el cuarto parámetro ("ACTIVO")
        Equipo equipo = new Equipo(nombreEquipo, jugador1, jugador2);

        // La línea equipo.setFechaCreacion(LocalDateTime.now()); es redundante,
        // ya que el constructor de 3 parámetros en Equipo.java ya lo hace.
        // Si la quieres mantener por seguridad, no hay problema, pero la he comentado aquí:
        // equipo.setFechaCreacion(LocalDateTime.now());

        return equipoDao.save(equipo);
    }
// ...

    @Transactional(readOnly = true)
    // Usamos Integer para los IDs de Usuario
    public boolean existeEquipoActivoEntreJugadores(Integer idJugador1, Integer idJugador2) {
        return equipoDao.existeEquipoActivoEntreJugadores(idJugador1, idJugador2);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para los IDs de Usuario
    public Optional<Equipo> buscarEquipoPorJugadores(Integer idJugador1, Integer idJugador2) {
        return equipoDao.findByJugadores(idJugador1, idJugador2);
    }

    @Transactional
    public void eliminarEquipo(Long idEquipo) {
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        equipoDao.delete(equipo);
    }

    @Transactional
    public void disolverEquipo(Long idEquipo) {
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        equipo.setEstado("DISUELTO");
        // Nota: Asume que la entidad Equipo tiene setFechaDisolucion
        // equipo.setFechaDisolucion(LocalDateTime.now());
        equipoDao.save(equipo);
    }

    @Transactional
    public void reactivarEquipo(Long idEquipo) {
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        if (!"DISUELTO".equals(equipo.getEstado())) {
            throw new IllegalStateException("Solo se pueden reactivar equipos disueltos");
        }

        // El DAO espera Integer
        boolean existeOtroEquipo = equipoDao.existeEquipoActivoEntreJugadores(
                equipo.getJugador1().getId_usuario(),
                equipo.getJugador2().getId_usuario()
        );

        if (existeOtroEquipo) {
            throw new IllegalStateException("Ya existe un equipo activo entre estos jugadores");
        }

        equipo.setEstado("ACTIVO");
        // Nota: Asume que la entidad Equipo tiene setFechaDisolucion
        // equipo.setFechaDisolucion(null);
        equipoDao.save(equipo);
    }

    @Transactional(readOnly = true)
    public List<Equipo> buscarEquiposPorNombre(String nombreEquipo) {
        return equipoDao.findByNombreEquipoContainingIgnoreCase(nombreEquipo);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public long contarEquiposActivosPorUsuario(Integer idUsuario) {
        return equipoDao.contarEquiposActivosPorUsuario(idUsuario);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public List<Equipo> obtenerEquiposCreadosPorUsuario(Integer idUsuario) {
        return equipoDao.findEquiposCreadosPorUsuario(idUsuario);
    }

    // --- Métodos de Utilidad (Requieren métodos en la entidad Equipo) ---

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public boolean esUsuarioMiembroDelEquipo(Long idEquipo, Integer idUsuario) {
        Equipo equipo = equipoDao.findById(idEquipo).orElse(null);
        if (equipo == null) {
            return false;
        }
        // Requiere: Equipo.esMiembro(idUsuario)
        return equipo.esMiembro(idUsuario);
    }

    @Transactional(readOnly = true)
    // Usamos Integer para el ID de Usuario
    public Usuario obtenerCompaneroEnEquipo(Long idEquipo, Integer idUsuario) {
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        // Requiere: Equipo.obtenerCompanero(idUsuario)
        return equipo.obtenerCompanero(idUsuario);
    }

    @Transactional
    public Equipo actualizarNombreEquipo(Long idEquipo, String nuevoNombre) {
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        equipo.setNombreEquipo(nuevoNombre);
        return equipoDao.save(equipo);
    }

    @Transactional(readOnly = true)
    public List<Equipo> obtenerEquiposActivosOrdenados() {
        return equipoDao.findByEstadoOrderByFechaCreacionDesc("ACTIVO");
    }
}