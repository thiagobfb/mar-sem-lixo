package com.marsemlixo.api.auth.controller.dto;

public record RefreshResponse(String accessToken, int expiresIn) {}
