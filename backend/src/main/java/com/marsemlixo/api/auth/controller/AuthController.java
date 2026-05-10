package com.marsemlixo.api.auth.controller;

import com.marsemlixo.api.auth.controller.dto.GoogleLoginRequest;
import com.marsemlixo.api.auth.controller.dto.RefreshResponse;
import com.marsemlixo.api.auth.controller.dto.TokenResponse;
import com.marsemlixo.api.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final int refreshExpiryDays;

    public AuthController(
            AuthService authService,
            @Value("${app.refresh.expiry-days:30}") int refreshExpiryDays) {
        this.authService = authService;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult result = authService.loginWithGoogle(request.idToken());
        setRefreshTokenCookie(response, result.refreshTokenRaw(), refreshExpiryDays * 86400);
        return ResponseEntity.ok(new TokenResponse(
                result.accessToken(), result.expiresIn(), result.voluntario()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.RefreshResult result = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, result.newRefreshTokenRaw(), refreshExpiryDays * 86400);
        return ResponseEntity.ok(new RefreshResponse(result.accessToken(), result.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String tokenRaw, int maxAgeSeconds) {
        Cookie cookie = new Cookie(COOKIE_NAME, tokenRaw);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
