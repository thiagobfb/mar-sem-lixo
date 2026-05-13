package com.marsemlixo.api.residuo.controller;

import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoCreateRequest;
import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoResponse;
import com.marsemlixo.api.residuo.service.RegistroResiduoCreateResult;
import com.marsemlixo.api.residuo.service.RegistroResiduoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Registros de Resíduo", description = "Registro de resíduos coletados em mutirões")
public class RegistroResiduoController {

    private final RegistroResiduoService registroResiduoService;

    public RegistroResiduoController(RegistroResiduoService registroResiduoService) {
        this.registroResiduoService = registroResiduoService;
    }

    @PostMapping("/registros-residuo")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Cria um registro de resíduo com idempotência por UUID")
    @ApiResponse(responseCode = "201", description = "Registro criado")
    @ApiResponse(responseCode = "200", description = "Registro já existia e foi retornado sem duplicação")
    @ApiResponse(responseCode = "409", description = "Mutirão não está EM_ANDAMENTO")
    public ResponseEntity<RegistroResiduoResponse> criar(@Valid @RequestBody RegistroResiduoCreateRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        Long voluntarioId = Long.parseLong(jwt.getSubject());
        RegistroResiduoCreateResult result = registroResiduoService.criar(request, voluntarioId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.response());
    }

    @GetMapping("/registros-residuo/{id}")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Retorna o detalhe de um registro de resíduo")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    public RegistroResiduoResponse buscarPorId(@PathVariable UUID id) {
        return registroResiduoService.buscarPorId(id);
    }

    @GetMapping("/mutiroes/{mutiraoId}/registros-residuo")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Lista registros de resíduo de um mutirão")
    @ApiResponse(responseCode = "404", description = "Mutirão não encontrado")
    public List<RegistroResiduoResponse> listarPorMutirao(@PathVariable Long mutiraoId) {
        return registroResiduoService.listarPorMutirao(mutiraoId);
    }
}
