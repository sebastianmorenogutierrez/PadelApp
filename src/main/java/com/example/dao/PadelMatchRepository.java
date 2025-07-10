package com.example.dao;

import com.example.domain.PadelMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PadelMatchRepository extends JpaRepository<PadelMatch, Long> {
    // You can add custom query methods here if needed
}