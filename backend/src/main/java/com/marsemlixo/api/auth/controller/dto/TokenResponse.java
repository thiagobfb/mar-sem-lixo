package com.marsemlixo.api.auth.controller.dto;

public record TokenResponse(String accessToken, int expiresIn, VoluntarioInfo voluntario) {}
