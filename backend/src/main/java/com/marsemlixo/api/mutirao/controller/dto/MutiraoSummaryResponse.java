package com.marsemlixo.api.mutirao.controller.dto;

import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record MutiraoSummaryResponse(
        Long id,
        String titulo,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        MutiraoStatus status,
        AreaResumo area
) {
    public record AreaResumo(Long id, String nome, AreaTipo tipo, String municipio) {}
}
