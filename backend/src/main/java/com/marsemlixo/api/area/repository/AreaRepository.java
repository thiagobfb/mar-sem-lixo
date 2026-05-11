package com.marsemlixo.api.area.repository;

import com.marsemlixo.api.area.domain.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface AreaRepository extends JpaRepository<Area, Long>, JpaSpecificationExecutor<Area> {

    boolean existsByNomeAndMunicipio(String nome, String municipio);

    boolean existsByNomeAndMunicipioAndIdNot(String nome, String municipio, Long id);
}
