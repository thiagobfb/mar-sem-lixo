package com.marsemlixo.api.residuo.controller;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.google.client-id=test-client-id",
                "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
        })
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class RegistroResiduoControllerIT {

    private static final String JWT_SECRET = "test-secret-key-must-be-at-least-32-bytes!";
    private static final String BASE_URL = "/api/registros-residuo";

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long coordenadorId;
    private Long voluntarioId;
    private Long areaId;
    private Long mutiraoId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE registro_residuo, mutirao, refresh_token, voluntario, area");

        coordenadorId = jdbcTemplate.queryForObject(
                "INSERT INTO voluntario (google_id, email, nome, role, data_cadastro) VALUES (?, ?, ?, ?, NOW()) RETURNING id",
                Long.class,
                "google-coord-id", "coord@test.com", "Coordenador Teste", "COORDENADOR");

        voluntarioId = jdbcTemplate.queryForObject(
                "INSERT INTO voluntario (google_id, email, nome, role, data_cadastro) VALUES (?, ?, ?, ?, NOW()) RETURNING id",
                Long.class,
                "google-vol-id", "vol@test.com", "Voluntário Teste", "VOLUNTARIO");

        areaId = Long.parseLong(extrairId(postComToken("/api/areas", payloadArea(), criarToken(coordenadorId, "COORDENADOR")).getBody()));
        mutiraoId = Long.parseLong(extrairId(postComToken("/api/mutiroes", payloadMutirao(), criarToken(coordenadorId, "COORDENADOR")).getBody()));
        patchStatus(mutiraoId, "EM_ANDAMENTO", criarToken(coordenadorId, "COORDENADOR"));
    }

    @Test
    void criar_comPayloadValido_retorna201() {
        String body = payloadRegistro(UUID.randomUUID(), mutiraoId);

        ResponseEntity<String> response = postComToken(BASE_URL, body, criarToken(voluntarioId, "VOLUNTARIO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("PLASTICO");
        assertThat(response.getBody()).contains("areaTotal");
    }

    @Test
    void criar_reenvioMesmoId_retorna200SemDuplicar() {
        UUID id = UUID.randomUUID();
        String body = payloadRegistro(id, mutiraoId);
        String token = criarToken(voluntarioId, "VOLUNTARIO");

        ResponseEntity<String> first = postComToken(BASE_URL, body, token);
        ResponseEntity<String> second = postComToken(BASE_URL, body, token);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registro_residuo WHERE id = ?::uuid",
                Integer.class,
                id.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void criar_comMutiraoPlanejado_retorna409() {
        Long planejadoId = Long.parseLong(extrairId(
                postComToken("/api/mutiroes", payloadMutiraoComTitulo("Planejado"), criarToken(coordenadorId, "COORDENADOR")).getBody()));

        ResponseEntity<String> response = postComToken(
                BASE_URL,
                payloadRegistro(UUID.randomUUID(), planejadoId),
                criarToken(voluntarioId, "VOLUNTARIO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void criar_semToken_retorna401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL,
                new HttpEntity<>(payloadRegistro(UUID.randomUUID(), mutiraoId), headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void buscarPorId_existente_retorna200() {
        UUID id = UUID.randomUUID();
        postComToken(BASE_URL, payloadRegistro(id, mutiraoId), criarToken(voluntarioId, "VOLUNTARIO"));

        ResponseEntity<String> response = getComToken(BASE_URL + "/" + id, criarToken(voluntarioId, "VOLUNTARIO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(id.toString());
    }

    @Test
    void listarPorMutirao_retorna200() {
        postComToken(BASE_URL, payloadRegistro(UUID.randomUUID(), mutiraoId), criarToken(voluntarioId, "VOLUNTARIO"));

        ResponseEntity<String> response = getComToken(
                "/api/mutiroes/" + mutiraoId + "/registros-residuo",
                criarToken(coordenadorId, "COORDENADOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PLASTICO");
    }

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

    private String payloadRegistro(UUID id, Long targetMutiraoId) {
        return """
                {
                  "id": "%s",
                  "mutiraoId": %s,
                  "tipoResiduo": "PLASTICO",
                  "metragemPerpendicular": 2.5,
                  "metragemTransversal": 1.2,
                  "quantidade": 3,
                  "localizacao": {
                    "type": "Point",
                    "coordinates": [-42.0432, -22.8791]
                  },
                  "fotoUrl": "https://example.com/foto.jpg",
                  "dataRegistro": "2025-06-14T10:32:11Z"
                }
                """.formatted(id, targetMutiraoId);
    }

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

    private ResponseEntity<String> patchStatus(Long targetMutiraoId, String novoStatus, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String body = "{\"status\": \"" + novoStatus + "\"}";
        return restTemplate.exchange(
                "/api/mutiroes/" + targetMutiraoId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private String criarToken(Long userId, String role) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("role", role)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extrairId(String body) {
        int idx = body.indexOf("\"id\":");
        int comma = body.indexOf(",", idx);
        String raw = body.substring(idx + 5, comma).replaceAll("[^0-9]", "");
        return raw;
    }
}
