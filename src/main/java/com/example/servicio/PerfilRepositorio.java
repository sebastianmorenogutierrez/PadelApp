package com.example.repositorio;

import com.example.domain.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepositorio extends JpaRepository<Perfil, Integer> {
}
