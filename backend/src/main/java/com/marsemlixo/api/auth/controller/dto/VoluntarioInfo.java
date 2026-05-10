package com.marsemlixo.api.auth.controller.dto;

import com.marsemlixo.api.auth.domain.VoluntarioRole;

import java.util.UUID;

public record VoluntarioInfo(UUID id, String nome, String email, VoluntarioRole role) {}
