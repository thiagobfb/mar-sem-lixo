package com.marsemlixo.api.relatorio.controller.dto;

import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.residuo.domain.TipoResiduo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RelatorioMutiraoResponse(
        MutiraoResumo mutirao,
        ResumoConsolidado resumo,
        List<TotalPorTipo> totaisPorTipo
) {

    public record MutiraoResumo(
            Long id,
            String titulo,
            LocalDate data,
            MutiraoStatus status,
            AreaResumo area
    ) {
    }

    public record AreaResumo(
            Long id,
            String nome,
            AreaTipo tipo,
            String municipio,
            String estado
    ) {
    }

    public record ResumoConsolidado(
            int totalRegistros,
            int totalItens,
            BigDecimal areaTotal,
            int voluntariosDistintos,
            Instant primeiroRegistroEm,
            Instant ultimoRegistroEm
    ) {
    }

    public record TotalPorTipo(
            TipoResiduo tipoResiduo,
            int totalRegistros,
            int totalItens,
            BigDecimal areaTotal
    ) {
    }
}
