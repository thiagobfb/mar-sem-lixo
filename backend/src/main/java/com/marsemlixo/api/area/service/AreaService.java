package com.marsemlixo.api.area.service;

import com.marsemlixo.api.area.controller.dto.AreaCreateRequest;
import com.marsemlixo.api.area.controller.dto.AreaResponse;
import com.marsemlixo.api.area.controller.dto.AreaUpdateRequest;
import com.marsemlixo.api.area.domain.AreaTipo;

import java.util.List;

public interface AreaService {

    AreaResponse criar(AreaCreateRequest request);

    AreaResponse buscarPorId(Long id);

    List<AreaResponse> listar(Boolean ativa, AreaTipo tipo, String municipio, String estado, boolean incluirPoligono);

    AreaResponse atualizar(Long id, AreaUpdateRequest request);

    void inativar(Long id);
}
