package com.example.domain.torneo;
import com.example.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    boolean existsByTorneoAndUsuario(Torneo torneo, Usuario usuario);
}