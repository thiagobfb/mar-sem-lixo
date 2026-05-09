package com.marsemlixo.api.area.controller;

import com.marsemlixo.api.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AreaControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE TABLE area");
    }

    private static final String BASE_URL = "/api/areas";

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
        ResponseEntity<String> response = post(PAYLOAD_VALIDO.replace("Praia do Forte", "Praia do Forte"));
        // segunda criação com mesmo nome+municipio => 409
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
        ResponseEntity<String> response = restTemplate.getForEntity(
                BASE_URL + "/" + UUID.randomUUID(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listar_retorna200() {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void inativar_existente_retorna204() {
        ResponseEntity<String> criacao = post(PAYLOAD_VALIDO.replace("Praia do Forte", "Praia Para Inativar"));
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String id = extrairId(criacao.getBody());
        ResponseEntity<Void> delete = restTemplate.exchange(
                BASE_URL + "/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void inativar_inexistente_retorna404() {
        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/" + UUID.randomUUID(), HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void atualizar_parcialmente_retorna200() {
        ResponseEntity<String> criacao = post(PAYLOAD_VALIDO.replace("Praia do Forte", "Praia Para Editar"));
        String id = extrairId(criacao.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        return restTemplate.postForEntity(BASE_URL, new HttpEntity<>(body, headers), String.class);
    }

    private String extrairId(String json) {
        // extrai "id":"<uuid>" do JSON de resposta
        int start = json.indexOf("\"id\":\"") + 6;
        return json.substring(start, start + 36);
    }
}
