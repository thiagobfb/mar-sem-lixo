package com.marsemlixo.api.mutirao.service;

import com.marsemlixo.api.mutirao.controller.dto.MutiraoCreateRequest;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoSummaryResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoUpdateRequest;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface MutiraoService {

    MutiraoResponse criar(MutiraoCreateRequest request, Long organizadorId);

    MutiraoResponse buscarPorId(Long id);

    Page<MutiraoSummaryResponse> listar(MutiraoStatus status, Long areaId,
                                        LocalDate dataInicio, LocalDate dataFim,
                                        Pageable pageable);

    MutiraoResponse atualizar(Long id, MutiraoUpdateRequest request);

    MutiraoResponse transicionarStatus(Long id, MutiraoStatus novoStatus);
}
