package com.marsemlixo.api.area.controller;

import com.marsemlixo.api.area.controller.dto.AreaCreateRequest;
import com.marsemlixo.api.area.controller.dto.AreaResponse;
import com.marsemlixo.api.area.controller.dto.AreaUpdateRequest;
import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.area.service.AreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@Tag(name = "Áreas", description = "Gerenciamento de áreas geográficas de coleta")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Cadastra uma nova área")
    @ApiResponse(responseCode = "201", description = "Área criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou polígono mal formado")
    @ApiResponse(responseCode = "409", description = "Nome já existe no município")
    public AreaResponse criar(@Valid @RequestBody AreaCreateRequest request) {
        return areaService.criar(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Lista áreas com filtros opcionais")
    public List<AreaResponse> listar(
            @RequestParam(required = false) Boolean ativa,
            @RequestParam(required = false) AreaTipo tipo,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "false") boolean incluirPoligono) {
        return areaService.listar(ativa, tipo, municipio, estado, incluirPoligono);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VOLUNTARIO', 'COORDENADOR')")
    @Operation(summary = "Retorna o detalhe de uma área")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    public AreaResponse buscarPorId(@PathVariable Long id) {
        return areaService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Atualiza parcialmente uma área")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    @ApiResponse(responseCode = "409", description = "Nome já existe no município")
    public AreaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AreaUpdateRequest request) {
        return areaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COORDENADOR')")
    @Operation(summary = "Inativa uma área (soft-delete)")
    @ApiResponse(responseCode = "204", description = "Área inativada com sucesso")
    @ApiResponse(responseCode = "404", description = "Área não encontrada")
    public void inativar(@PathVariable Long id) {
        areaService.inativar(id);
    }
}
