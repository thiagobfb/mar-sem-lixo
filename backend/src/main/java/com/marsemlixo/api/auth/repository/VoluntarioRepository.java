package com.marsemlixo.api.auth.repository;

import com.marsemlixo.api.auth.domain.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    Optional<Voluntario> findByGoogleId(String googleId);
}
