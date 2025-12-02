package com.example.servicio;

import com.example.dao.TorneoDao;
import com.example.domain.torneo.Participante;
import com.example.domain.torneo.ParticipanteRepository;
import com.example.domain.torneo.Torneo;
import com.example.domain.torneo.TorneoRepository;
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

    // Métodos usando TorneoDao
    @Transactional(readOnly = true)
    public List<Torneo> buscarPorNombre(String nombre) {
        return torneoDao.findByNombreContainingIgnoreCase(nombre);
    }

    @Transactional(readOnly = true)
    public List<Torneo> buscarTorneosActivos() {
        return torneoDao.findByActivoTrue();
    }

    // Métodos usando TorneoRepository
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

    @Transactional
    public void eliminar(Torneo torneo) {
        torneoRepository.delete(torneo);
    }

    @Transactional(readOnly = true)
    public Optional<Torneo> buscarPorId(Long id) {
        return torneoRepository.findById(id);
    }

    @Transactional
    public void inscribirPersona(Long torneoId, String nombre) {
        Torneo torneo = torneoRepository.findById(torneoId).orElseThrow();
        Participante participante = new Participante();
        participante.setNombre(nombre);
        participante.setTorneo(torneo);
        participanteRepository.save(participante);
    }
}