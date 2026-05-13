package com.marsemlixo.api.area.controller;

import com.marsemlixo.api.TestJwksConfig;
import com.marsemlixo.api.TestcontainersConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.google.client-id=test-client-id",
                "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
        })
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class AreaControllerIT {

    private static final String JWT_SECRET = "test-secret-key-must-be-at-least-32-bytes!";
    private static final String BASE_URL = "/api/areas";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE TABLE registro_residuo, mutirao, area");
    }

    private static final String PAYLOAD_VALIDO = """
            {
              "nome": "Praia do Forte",
              "tipo": "PRAIA",
              "municipio": "Cabo Frio",
              "estado": "RJ",
              "poligono": {
                "type": "Polygon",
                "coordinates": [[
                  [-42.0, -22.0],
                  [-42.1, -22.0],
                  [-42.1, -22.1],
                  [-42.0, -22.1],
                  [-42.0, -22.0]
                ]]
              }
            }
            """;

    @Test
    void criar_comPayloadValido_retorna201() {
        ResponseEntity<String> response = post(PAYLOAD_VALIDO);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Praia do Forte");
    }

    @Test
    void criar_comNomeDuplicado_retorna409() {
        post(PAYLOAD_VALIDO);
        ResponseEntity<String> dupe = post(PAYLOAD_VALIDO);
        assertThat(dupe.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void criar_comCamposObrigatoriosFaltando_retorna400() {
        String payloadInvalido = """
                { "nome": "", "tipo": "PRAIA" }
                """;
        ResponseEntity<String> response = post(payloadInvalido);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void buscarPorId_inexistente_retorna404() {
        ResponseEntity<String> response = get(BASE_URL + "/999999");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listar_retorna200() {
        ResponseEntity<String> response = get(BASE_URL);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void inativar_existente_retorna204() {
        ResponseEntity<String> criacao = post(PAYLOAD_VALIDO.replace("Praia do Forte", "Praia Para Inativar"));
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String id = extrairId(criacao.getBody());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(criarTokenCoordenador());
        ResponseEntity<Void> delete = restTemplate.exchange(
                BASE_URL + "/" + id, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void inativar_inexistente_retorna404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(criarTokenCoordenador());
        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/999999", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void atualizar_parcialmente_retorna200() {
        ResponseEntity<String> criacao = post(PAYLOAD_VALIDO.replace("Praia do Forte", "Praia Para Editar"));
        String id = extrairId(criacao.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(criarTokenCoordenador());
        HttpEntity<String> patch = new HttpEntity<>("""
                { "tipo": "LAGOA" }
                """, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/" + id, HttpMethod.PATCH, patch, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("LAGOA");
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(criarTokenCoordenador());
        return restTemplate.postForEntity(BASE_URL, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(criarTokenCoordenador());
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String extrairId(String json) {
        int start = json.indexOf("\"id\":") + 5;
        String tail = json.substring(start).trim();
        int end = tail.indexOf(",");
        if (end == -1) end = tail.indexOf("}");
        return tail.substring(0, end).trim();
    }

    private String criarTokenCoordenador() {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("1")
                    .claim("email", "coord@test.com")
                    .claim("role", "COORDENADOR")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar token de coordenador para teste", e);
        }
    }
}
