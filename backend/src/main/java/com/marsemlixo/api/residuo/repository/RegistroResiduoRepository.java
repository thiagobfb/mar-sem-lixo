package com.marsemlixo.api.residuo.repository;

import com.marsemlixo.api.residuo.domain.RegistroResiduo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroResiduoRepository extends JpaRepository<RegistroResiduo, UUID> {
    List<RegistroResiduo> findByMutiraoIdOrderByDataRegistroAsc(Long id);
}
