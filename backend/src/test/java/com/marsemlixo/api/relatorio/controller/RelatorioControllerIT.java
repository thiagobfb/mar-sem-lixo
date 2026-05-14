package com.marsemlixo.api.relatorio.controller;

import com.marsemlixo.api.TestJwksConfig;
import com.marsemlixo.api.TestcontainersConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.JsonPathExpectationsHelper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.google.client-id=test-client-id",
                "app.jwt.secret=test-secret-key-must-be-at-least-32-bytes!"
        })
@Import({TestcontainersConfig.class, TestJwksConfig.class})
class RelatorioControllerIT {

    private static final String JWT_SECRET = "test-secret-key-must-be-at-least-32-bytes!";

    @Autowired org.springframework.boot.test.web.client.TestRestTemplate restTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long coordenadorId;
    private Long voluntarioId;
    private Long areaId;
    private Long mutiraoConcluidoId;
    private Long mutiraoEmAndamentoId;

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

        areaId = jdbcTemplate.queryForObject("""
                INSERT INTO area (nome, tipo, municipio, estado, poligono, ativa)
                VALUES (?, ?, ?, ?, ST_GeomFromText(?, 4326), true)
                RETURNING id
                """, Long.class,
                "Praia do Forte", "PRAIA", "Cabo Frio", "RJ",
                "POLYGON((-42.0 -22.0,-42.1 -22.0,-42.1 -22.1,-42.0 -22.1,-42.0 -22.0))");

        mutiraoConcluidoId = inserirMutirao("Mutirão Concluído", "CONCLUIDO");
        mutiraoEmAndamentoId = inserirMutirao("Mutirão Em Andamento", "EM_ANDAMENTO");

        jdbcTemplate.update("""
                INSERT INTO registro_residuo (
                    id, mutirao_id, voluntario_id, tipo_residuo,
                    metragem_perpendicular, metragem_transversal, quantidade,
                    area_total, localizacao, foto_url, data_registro, synced_at
                ) VALUES (
                    CAST(? AS uuid), ?, ?, ?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?, ?
                )
                """,
                "6cd3c226-2eb3-4f8f-bcf1-3efabebc1a11", mutiraoConcluidoId, voluntarioId, "PLASTICO",
                2.50, 1.20, 3, 9.00, -42.0432, -22.8791, "https://example.com/foto.jpg",
                Timestamp.from(Instant.parse("2025-06-14T10:32:11Z")),
                Timestamp.from(Instant.parse("2025-06-14T10:33:02Z")));
    }

    @Test
    void resumirMutirao_comCoordenador_retorna200() {
        ResponseEntity<String> response = getComToken(
                "/api/relatorios/mutiroes/" + mutiraoConcluidoId,
                criarToken(coordenadorId, "COORDENADOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        new JsonPathExpectationsHelper("$.resumo.totalRegistros").assertValue(response.getBody(), 1);
        new JsonPathExpectationsHelper("$.resumo.totalItens").assertValue(response.getBody(), 3);
        new JsonPathExpectationsHelper("$.totaisPorTipo[0].tipoResiduo").assertValue(response.getBody(), "PLASTICO");
    }

    @Test
    void resumirMutirao_comVoluntario_retorna403() {
        ResponseEntity<String> response = getComToken(
                "/api/relatorios/mutiroes/" + mutiraoConcluidoId,
                criarToken(voluntarioId, "VOLUNTARIO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resumirMutirao_semToken_retorna401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/relatorios/mutiroes/" + mutiraoConcluidoId,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exportarExcel_comMutiraoConcluido_retornaArquivo() throws Exception {
        ResponseEntity<byte[]> response = getBytesComToken(
                "/api/relatorios/mutiroes/" + mutiraoConcluidoId + "/excel",
                criarToken(coordenadorId, "COORDENADOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("relatorio-mutirao-" + mutiraoConcluidoId + ".xlsx");
        assertThat(response.getBody()).isNotNull();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertThat(workbook.getSheet("Resumo")).isNotNull();
            assertThat(workbook.getSheet("Registros").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("6cd3c226-2eb3-4f8f-bcf1-3efabebc1a11");
        }
    }

    @Test
    void exportarExcel_comMutiraoNaoConcluido_retorna409() {
        ResponseEntity<String> response = getComToken(
                "/api/relatorios/mutiroes/" + mutiraoEmAndamentoId + "/excel",
                criarToken(coordenadorId, "COORDENADOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private Long inserirMutirao(String titulo, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO mutirao (titulo, data, hora_inicio, hora_fim, area_id, organizador_id, status, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                titulo, LocalDate.now().plusDays(7), Time.valueOf("08:00:00"), Time.valueOf("12:00:00"),
                areaId, coordenadorId, status, null);
    }

    private ResponseEntity<String> getComToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<byte[]> getBytesComToken(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
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
}
