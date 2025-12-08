package com.example.servicio;

import com.example.dao.PadelMatchDao;
import com.example.dao.PadelMatchRepository;
import com.example.domain.PadelMatch;
import com.example.domain.usuario.Usuario;
import com.example.domain.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PadelMatchService {

    private static final int MAX_JUGADORES = 4;

    @Autowired
    private PadelMatchDao padelMatchDao;

    @Autowired
    private PadelMatchRepository padelMatchRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Métodos de búsqueda (Usando PadelMatchDao)
    @Transactional(readOnly = true)
    public List<PadelMatch> buscarPorNombre(String nombre) {
        return padelMatchDao.findByNombrePartidoContainingIgnoreCase(nombre);
    }

    @Transactional(readOnly = true)
    public List<PadelMatch> buscarMatchesProximos() {
        return padelMatchDao.findMatchesProximos(LocalDate.now());
    }

    // Métodos CRUD básicos (Usando PadelMatchRepository)
    @Transactional(readOnly = true)
    public List<PadelMatch> listarTodos() {
        return padelMatchRepository.findAll();
    }

    @Transactional
    public PadelMatch guardar(PadelMatch match) {
        return padelMatchRepository.save(match);
    }

    @Transactional
    public void eliminar(Long id) {
        padelMatchRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PadelMatch> buscarPorId(Long id) {
        Optional<PadelMatch> match = padelMatchRepository.findById(id);

        // 💡 CORRECCIÓN CRÍTICA: Forzar la inicialización de la colección de jugadores
        // Esto soluciona que la lista de jugadores no se muestre en el HTML.
        match.ifPresent(m -> {
            if (m.getJugadores() != null) {
                m.getJugadores().size();
            }
        });

        return match;
    }

    @Transactional
    public PadelMatch crearPartidoYRegistrarCreador(PadelMatch match, Integer creadorId) {
        // 1. Buscar al usuario creador por su ID (usa Integer correctamente)
        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RuntimeException("Error: El usuario creador con ID " + creadorId + " no existe."));

        // 2. Asignar el creador al partido
        match.setCreador(creador);

        // 3. Guardar el partido primero para asegurar un ID
        PadelMatch matchGuardado = padelMatchRepository.save(match);

        // 4. Registrar al creador como el primer jugador del partido
        if (!matchGuardado.getJugadores().contains(creador)) {
            matchGuardado.getJugadores().add(creador);
        }

        // 5. Guardar de nuevo para persistir la lista de jugadores
        return padelMatchRepository.save(matchGuardado);
    }

    @Transactional
    public void inscribirJugador(Long matchId, Integer usuarioId) {
        // 1. Buscar el jugador
        Usuario jugador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado."));

        // 2. Buscar el partido
        PadelMatch match = padelMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado."));

        // 3. VALIDACIÓN: Límite de jugadores
        if (match.getJugadores().size() >= MAX_JUGADORES) {
            throw new RuntimeException("El partido ya está lleno (" + MAX_JUGADORES + " jugadores).");
        }

        // 4. VALIDACIÓN: Jugador ya inscrito
        if (match.getJugadores().contains(jugador)) {
            throw new RuntimeException("El jugador ya está inscrito en el partido.");
        }

        // 5. Inscribir y guardar
        match.getJugadores().add(jugador);
        padelMatchRepository.save(match);
    }
}