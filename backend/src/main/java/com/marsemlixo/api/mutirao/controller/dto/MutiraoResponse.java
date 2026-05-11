package com.marsemlixo.api.mutirao.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MutiraoResponse(
        Long id,
        String titulo,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        MutiraoStatus status,
        String observacoes,
        AreaResumo area,
        OrganizadorResumo organizador
) {
    public record AreaResumo(Long id, String nome, AreaTipo tipo, String municipio) {}

    public record OrganizadorResumo(Long id, String nome) {}
}
