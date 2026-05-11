package com.marsemlixo.api.mutirao.controller.dto;

import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import jakarta.validation.constraints.NotNull;

public record MutiraoStatusRequest(@NotNull MutiraoStatus status) {}
