package com.marsemlixo.api.auth.controller.dto;

import com.marsemlixo.api.auth.domain.VoluntarioRole;

public record VoluntarioInfo(Long id, String nome, String email, VoluntarioRole role) {}
