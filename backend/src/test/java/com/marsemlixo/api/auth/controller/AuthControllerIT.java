package com.marsemlixo.api.auth.controller;

import com.marsemlixo.api.TestJwksConfig;
import com.marsemlixo.api.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT,
        properties = {
                "app.google.client-id=test-client-id",
                "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
        })
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class AuthControllerIT {

    private static final String CLIENT_ID = "test-client-id";
    private static final String BASE_URL = "/api/auth";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpar() {
        jdbcTemplate.execute("TRUNCATE TABLE refresh_token, voluntario CASCADE");
    }

    @Test
    void login_comTokenValido_retorna200ComAccessToken() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-001", "teste@example.com", "Usuário Teste", CLIENT_ID);

        ResponseEntity<String> response = postLogin(idToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("accessToken");
        assertThat(response.getBody()).contains("expiresIn");
        assertThat(response.getBody()).contains("voluntario");
    }

    @Test
    void login_criaVoluntarioNoPrimeiroLogin() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-002", "novo@example.com", "Novo Voluntário", CLIENT_ID);

        postLogin(idToken);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM voluntario WHERE email = 'novo@example.com'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void login_comTokenInvalido_retorna401() {
        ResponseEntity<String> response = postLogin("token.invalido.aqui");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_comCookieValido_retornaNovoAccessToken() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-003", "refresh@example.com", "Refresh User", CLIENT_ID);
        ResponseEntity<String> loginResponse = postLogin(idToken);

        String refreshCookie = extrairRefreshCookie(loginResponse);
        assertThat(refreshCookie).isNotNull();

        ResponseEntity<String> refreshResponse = postRefresh(refreshCookie);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).contains("accessToken");
    }

    @Test
    void refresh_semCookie_retorna401() {
        ResponseEntity<String> response = postRefresh(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_limpaCookie() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-004", "logout@example.com", "Logout User", CLIENT_ID);
        ResponseEntity<String> loginResponse = postLogin(idToken);
        String refreshCookie = extrairRefreshCookie(loginResponse);

        ResponseEntity<Void> logoutResponse = postLogout(refreshCookie);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Verifica que o cookie foi limpo (maxAge=0)
        List<String> setCookies = logoutResponse.getHeaders().get("Set-Cookie");
        assertThat(setCookies).isNotNull();
        assertThat(setCookies.stream().anyMatch(c -> c.contains("Max-Age=0"))).isTrue();
    }

    @Test
    void fluxoCompleto_login_refresh_logout() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-005", "fluxo@example.com", "Fluxo User", CLIENT_ID);

        // Login
        ResponseEntity<String> loginResponse = postLogin(idToken);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String refreshCookie = extrairRefreshCookie(loginResponse);
        assertThat(refreshCookie).isNotNull();

        // Refresh
        ResponseEntity<String> refreshResponse = postRefresh(refreshCookie);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String novoRefreshCookie = extrairRefreshCookie(refreshResponse);
        assertThat(novoRefreshCookie).isNotNull();

        // Logout
        ResponseEntity<Void> logoutResponse = postLogout(novoRefreshCookie);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rotacao_tokenAntigoAposRefresh_retorna401() {
        String idToken = TestJwksConfig.criarGoogleIdToken(
                "google-sub-006", "rotacao@example.com", "Rotacao User", CLIENT_ID);
        ResponseEntity<String> loginResponse = postLogin(idToken);
        String tokenOriginal = extrairRefreshCookie(loginResponse);

        // Primeiro refresh consome o token original
        postRefresh(tokenOriginal);

        // Tentar usar o token original novamente deve falhar
        ResponseEntity<String> segundoRefresh = postRefresh(tokenOriginal);
        assertThat(segundoRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> postLogin(String idToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"idToken\":\"" + idToken + "\"}";
        return restTemplate.postForEntity(BASE_URL + "/google", new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> postRefresh(String refreshCookieValue) {
        HttpHeaders headers = new HttpHeaders();
        if (refreshCookieValue != null) {
            headers.add("Cookie", "refresh_token=" + refreshCookieValue);
        }
        return restTemplate.exchange(
                BASE_URL + "/refresh", HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<Void> postLogout(String refreshCookieValue) {
        HttpHeaders headers = new HttpHeaders();
        if (refreshCookieValue != null) {
            headers.add("Cookie", "refresh_token=" + refreshCookieValue);
        }
        return restTemplate.exchange(
                BASE_URL + "/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
    }

    private String extrairRefreshCookie(ResponseEntity<?> response) {
        List<String> setCookies = response.getHeaders().get("Set-Cookie");
        if (setCookies == null) return null;
        return setCookies.stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.split(";")[0].substring("refresh_token=".length()))
                .findFirst()
                .orElse(null);
    }
}
