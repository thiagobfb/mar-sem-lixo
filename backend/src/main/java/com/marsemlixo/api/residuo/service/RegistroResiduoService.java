package com.marsemlixo.api.residuo.service;

import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoCreateRequest;
import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoResponse;

import java.util.List;
import java.util.UUID;

public interface RegistroResiduoService {
    RegistroResiduoCreateResult criar(RegistroResiduoCreateRequest request, Long voluntarioId);
    RegistroResiduoResponse buscarPorId(UUID id);
    List<RegistroResiduoResponse> listarPorMutirao(Long mutiraoId);
}
