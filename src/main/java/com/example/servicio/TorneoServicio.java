package com.example.servicio;

import com.example.dao.TorneoDao;
import com.example.domain.torneo.Participante;
import com.example.domain.torneo.ParticipanteRepository;
import com.example.domain.torneo.Torneo;
import com.example.domain.torneo.TorneoRepository;
import com.example.domain.usuario.Usuario; // ¡Importante!
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TorneoServicio {

    @Autowired
    private TorneoDao torneoDao;

    @Autowired
    private TorneoRepository torneoRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    // Importante: Necesitas el UsuarioServicio si quieres obtener el usuario actual
    // @Autowired
    // private UsuarioServicio usuarioServicio;

    // ... (Métodos listarTodos, guardar, eliminar, buscarPorId) ...
    // Dejo solo los relevantes para simplificar, el resto de tu código debe ir aquí.

    @Transactional(readOnly = true)
    public List<Torneo> listarTodos() {
        return torneoRepository.findAll();
    }

    @Transactional
    public Torneo guardar(Torneo torneo) {
        return torneoRepository.save(torneo);
    }

    @Transactional
    public void eliminar(Long id) {
        torneoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Torneo> buscarPorId(Long id) {
        return torneoRepository.findById(id);
    }

    // 🏆 NUEVO MÉTODO 1: Inscripción con Validación Única
    @Transactional
    public void inscribirUsuario(Long torneoId, Usuario usuario) {
        if (usuario == null || usuario.getId_usuario() == null) {
            throw new IllegalStateException("El usuario debe estar logueado para inscribirse.");
        }

        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalStateException("Torneo no encontrado con ID: " + torneoId));

        // Validación de inscripción única
        if (esParticipante(torneo, usuario)) {
            throw new IllegalStateException("Ya estás inscrito en el torneo: " + torneo.getNombre());
        }

        // Crear el nuevo participante
        Participante participante = new Participante();

        // Obtener el nombre completo del individuo asociado al usuario, si existe
        String nombreParticipante = (usuario.getIndividuo() != null)
                ? usuario.getIndividuo().getNombre() + " " + usuario.getIndividuo().getApellido()
                : usuario.getNombreUsuario(); // Fallback si no hay Individuo asociado

        participante.setNombre(nombreParticipante);
        participante.setTorneo(torneo);
        participante.setUsuario(usuario); // Asignar el objeto Usuario

        participanteRepository.save(participante);
    }

    // 🏆 NUEVO MÉTODO 2: Verificación de Participación
    @Transactional(readOnly = true)
    public boolean esParticipante(Torneo torneo, Usuario usuario) {
        if (usuario == null || usuario.getId_usuario() == null) {
            return false;
        }
        // Requiere: ParticipanteRepository.existsByTorneoAndUsuario(Torneo torneo, Usuario usuario);
        return participanteRepository.existsByTorneoAndUsuario(torneo, usuario);
    }
}