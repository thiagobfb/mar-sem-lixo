package com.marsemlixo.api.mutirao.controller;

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
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.google.client-id=test-client-id",
                "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
        })
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class MutiraoControllerIT {

    private static final String JWT_SECRET = "test-secret-key-must-be-at-least-32-bytes!";
    private static final String BASE_URL = "/api/mutiroes";

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long organizadorId;
    private Long areaId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE registro_residuo, mutirao, refresh_token, voluntario, area");

        organizadorId = jdbcTemplate.queryForObject(
                "INSERT INTO voluntario (google_id, email, nome, role, data_cadastro) VALUES (?, ?, ?, ?, NOW()) RETURNING id",
                Long.class,
                "google-test-id", "coord@test.com", "Coordenador Teste", "COORDENADOR");

        ResponseEntity<String> areaResponse = postComToken("/api/areas", payloadArea(), criarTokenCoordenador());
        areaId = Long.parseLong(extrairId(areaResponse.getBody()));
    }

    @Test
    void criar_comPayloadValido_retorna201() {
        ResponseEntity<String> response = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("PLANEJADO");
        assertThat(response.getBody()).contains("Mutirão Praia do Forte");
    }

    @Test
    void criar_comCamposObrigatoriosFaltando_retorna400() {
        ResponseEntity<String> response = postComToken(BASE_URL, "{\"titulo\":\"\"}", criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criar_comHoraFimAnteriorHoraInicio_retorna400() {
        String payload = payloadMutiraoComHorario("12:00", "08:00");
        ResponseEntity<String> response = postComToken(BASE_URL, payload, criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criar_comAreaInexistente_retorna422() {
        String payload = payloadMutiraoComArea(999999L);
        ResponseEntity<String> response = postComToken(BASE_URL, payload, criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void criar_porVoluntario_retorna403() {
        ResponseEntity<String> response = postComToken(BASE_URL, payloadMutirao(), criarTokenVoluntario());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void buscarPorId_existente_retorna200() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = getComToken(BASE_URL + "/" + id, criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("area");
        assertThat(response.getBody()).contains("organizador");
    }

    @Test
    void buscarPorId_inexistente_retorna404() {
        ResponseEntity<String> response = getComToken(BASE_URL + "/999999", criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarPorId_porVoluntario_retorna200() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = getComToken(BASE_URL + "/" + id, criarTokenVoluntario());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listar_retorna200ComPaginacao() {
        postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());

        ResponseEntity<String> response = getComToken(BASE_URL, criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getBody()).contains("totalElements");
    }

    @Test
    void listar_porVoluntario_retorna200() {
        ResponseEntity<String> response = getComToken(BASE_URL, criarTokenVoluntario());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void transicionarStatus_planejadoParaEmAndamento_retorna200() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = patchStatusComToken(id, "EM_ANDAMENTO", criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("EM_ANDAMENTO");
    }

    @Test
    void transicionarStatus_cicloCompleto_retorna200() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> iniciar = patchStatusComToken(id, "EM_ANDAMENTO", criarTokenCoordenador());
        assertThat(iniciar.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> concluir = patchStatusComToken(id, "CONCLUIDO", criarTokenCoordenador());
        assertThat(concluir.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(concluir.getBody()).contains("CONCLUIDO");
    }

    @Test
    void transicionarStatus_transicaoInvalida_retorna409() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = patchStatusComToken(id, "CONCLUIDO", criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void transicionarStatus_estadoTerminalConcluido_retorna409() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());
        patchStatusComToken(id, "EM_ANDAMENTO", criarTokenCoordenador());
        patchStatusComToken(id, "CONCLUIDO", criarTokenCoordenador());

        ResponseEntity<String> response = patchStatusComToken(id, "CANCELADO", criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void transicionarStatus_porVoluntario_retorna403() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = patchStatusComToken(id, "EM_ANDAMENTO", criarTokenVoluntario());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void atualizar_mutiraoPlanejado_retorna200() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        String payloadAtualizado = payloadMutiraoComTitulo("Título Atualizado");
        ResponseEntity<String> response = putComToken(BASE_URL + "/" + id, payloadAtualizado, criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Título Atualizado");
    }

    @Test
    void atualizar_mutiraoEmAndamento_retorna409() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());
        patchStatusComToken(id, "EM_ANDAMENTO", criarTokenCoordenador());

        ResponseEntity<String> response = putComToken(BASE_URL + "/" + id, payloadMutirao(), criarTokenCoordenador());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void atualizar_porVoluntario_retorna403() {
        ResponseEntity<String> criacao = postComToken(BASE_URL, payloadMutirao(), criarTokenCoordenador());
        String id = extrairId(criacao.getBody());

        ResponseEntity<String> response = putComToken(BASE_URL + "/" + id, payloadMutirao(), criarTokenVoluntario());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- helpers de payload ----

    private String payloadArea() {
        return """
                {
                  "nome": "Praia do Forte",
                  "tipo": "PRAIA",
                  "municipio": "Cabo Frio",
                  "estado": "RJ",
                  "poligono": {
                    "type": "Polygon",
                    "coordinates": [[[-42.0,-22.0],[-42.1,-22.0],[-42.1,-22.1],[-42.0,-22.1],[-42.0,-22.0]]]
                  }
                }
                """;
    }

    private String payloadMutirao() {
        return payloadMutiraoComTitulo("Mutirão Praia do Forte");
    }

    private String payloadMutiraoComTitulo(String titulo) {
        return """
                {
                  "titulo": "%s",
                  "data": "%s",
                  "horaInicio": "08:00",
                  "horaFim": "12:00",
                  "areaId": %s
                }
                """.formatted(titulo, LocalDate.now().plusDays(7), areaId);
    }

    private String payloadMutiraoComHorario(String inicio, String fim) {
        return """
                {
                  "titulo": "Título",
                  "data": "%s",
                  "horaInicio": "%s",
                  "horaFim": "%s",
                  "areaId": %s
                }
                """.formatted(LocalDate.now().plusDays(7), inicio, fim, areaId);
    }

    private String payloadMutiraoComArea(Long outroAreaId) {
        return """
                {
                  "titulo": "Título",
                  "data": "%s",
                  "horaInicio": "08:00",
                  "horaFim": "12:00",
                  "areaId": %s
                }
                """.formatted(LocalDate.now().plusDays(7), outroAreaId);
    }

    // ---- helpers HTTP ----

    private ResponseEntity<String> postComToken(String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> getComToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> patchStatusComToken(String mutiraoId, String novoStatus, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String body = "{\"status\": \"" + novoStatus + "\"}";
        return restTemplate.exchange(
                BASE_URL + "/" + mutiraoId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private ResponseEntity<String> putComToken(String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private String extrairId(String json) {
        int start = json.indexOf("\"id\":") + 5;
        String tail = json.substring(start).trim();
        int end = tail.indexOf(",");
        if (end == -1) end = tail.indexOf("}");
        return tail.substring(0, end).trim();
    }

    // ---- helpers de token ----

    private String criarTokenCoordenador() {
        return criarToken(organizadorId, "COORDENADOR");
    }

    private String criarTokenVoluntario() {
        return criarToken(999999L, "VOLUNTARIO");
    }

    private String criarToken(Long subject, String role) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .claim("role", role)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar token de teste", e);
        }
    }
}
