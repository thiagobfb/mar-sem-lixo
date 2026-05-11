package com.marsemlixo.api.mutirao.controller.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record MutiraoCreateRequest(
        @NotBlank String titulo,
        @NotNull @FutureOrPresent LocalDate data,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull Long areaId,
        String observacoes
) {}
