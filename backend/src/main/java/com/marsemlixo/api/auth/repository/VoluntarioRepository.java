package com.marsemlixo.api.auth.repository;

import com.marsemlixo.api.auth.domain.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoluntarioRepository extends JpaRepository<Voluntario, UUID> {
    Optional<Voluntario> findByGoogleId(String googleId);
}
