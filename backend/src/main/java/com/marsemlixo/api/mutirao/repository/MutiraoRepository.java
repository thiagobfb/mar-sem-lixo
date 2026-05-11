package com.marsemlixo.api.mutirao.repository;

import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface  MutiraoRepository extends JpaRepository<Mutirao, Long>, JpaSpecificationExecutor<Mutirao> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Mutirao m SET m.status = :novoStatus WHERE m.id = :id AND m.status = :statusAtual")
    int transicionarStatus(@Param("id") Long id,
                           @Param("statusAtual") MutiraoStatus statusAtual,
                           @Param("novoStatus") MutiraoStatus novoStatus);
}
