package com.marsemlixo.api.mutirao.controller;

import com.marsemlixo.api.mutirao.controller.dto.MutiraoCreateRequest;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoStatusRequest;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoSummaryResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoUpdateRequest;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.service.MutiraoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/mutiroes")
@Tag(name = "Mutirões", description = "Gerenciamento de mutirões de coleta")
public class MutiraoController {

    private final MutiraoService mutiraoService;

    public MutiraoController(MutiraoService mutiraoService) {
        this.mutiraoService = mutiraoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Cria um novo mutirão")
    @ApiResponse(responseCode = "201", description = "Mutirão criado com status PLANEJADO")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou horaFim ≤ horaInicio")
    @ApiResponse(responseCode = "422", description = "Área não encontrada ou inativa")
    public MutiraoResponse criar(@Valid @RequestBody MutiraoCreateRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        Long organizadorId = Long.parseLong(jwt.getSubject());
        return mutiraoService.criar(request, organizadorId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Lista mutirões com filtros opcionais")
    public Page<MutiraoSummaryResponse> listar(
            @RequestParam(required = false) MutiraoStatus status,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @PageableDefault(size = 10, sort = {"data", "horaInicio"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return mutiraoService.listar(status, areaId, dataInicio, dataFim, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Retorna o detalhe de um mutirão")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    public MutiraoResponse buscarPorId(@PathVariable Long id) {
        return mutiraoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Atualiza dados de um mutirão PLANEJADO")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    @ApiResponse(responseCode = "409", description = "Mutirão não está no status PLANEJADO")
    @ApiResponse(responseCode = "422", description = "Área não encontrada ou inativa")
    public MutiraoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody MutiraoUpdateRequest request) {
        return mutiraoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Transiciona o status de um mutirão")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    @ApiResponse(responseCode = "409", description = "Transição de status inválida")
    public MutiraoResponse transicionarStatus(@PathVariable Long id,
                                              @Valid @RequestBody MutiraoStatusRequest request) {
        return mutiraoService.transicionarStatus(id, request.status());
    }
}
