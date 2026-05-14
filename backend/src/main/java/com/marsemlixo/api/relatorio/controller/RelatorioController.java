package com.marsemlixo.api.relatorio.controller;

import com.marsemlixo.api.relatorio.controller.dto.RelatorioMutiraoResponse;
import com.marsemlixo.api.relatorio.service.RelatorioExcelFile;
import com.marsemlixo.api.relatorio.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Resumo consolidado e exportação Excel de mutirões")
public class RelatorioController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/mutiroes/{mutiraoId}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Retorna o resumo consolidado de um mutirão")
    @ApiResponse(responseCode = "200", description = "Resumo gerado")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    public RelatorioMutiraoResponse resumirMutirao(@PathVariable Long mutiraoId) {
        return relatorioService.gerarResumoPorMutirao(mutiraoId);
    }

    @GetMapping("/mutiroes/{mutiraoId}/excel")
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Exporta o relatório Excel de um mutirão concluído")
    @ApiResponse(responseCode = "200", description = "Arquivo Excel gerado")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    @ApiResponse(responseCode = "409", description = "Mutirão ainda não está concluído")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable Long mutiraoId) {
        RelatorioExcelFile arquivo = relatorioService.exportarExcelPorMutirao(mutiraoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(EXCEL_MEDIA_TYPE);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(arquivo.fileName())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(arquivo.content());
    }
}
