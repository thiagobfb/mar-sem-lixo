package com.marsemlixo.api.auth.service;

import com.marsemlixo.api.auth.controller.dto.VoluntarioInfo;

public interface AuthService {

    record LoginResult(String accessToken, int expiresIn, String refreshTokenRaw, VoluntarioInfo voluntario) {}

    record RefreshResult(String accessToken, int expiresIn, String newRefreshTokenRaw) {}

    LoginResult loginWithGoogle(String idToken);

    RefreshResult refresh(String refreshTokenRaw);

    void logout(String refreshTokenRaw);
}
