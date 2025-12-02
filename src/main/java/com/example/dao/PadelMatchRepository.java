package com.example.dao;

import com.example.domain.PadelMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PadelMatchRepository extends JpaRepository<PadelMatch, Long> {
    // Puedes añadir métodos aquí si necesitas buscar por estado u otra propiedad
}