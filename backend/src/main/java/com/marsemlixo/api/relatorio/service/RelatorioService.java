package com.marsemlixo.api.relatorio.service;

import com.marsemlixo.api.relatorio.controller.dto.RelatorioMutiraoResponse;

public interface RelatorioService {

    RelatorioMutiraoResponse gerarResumoPorMutirao(Long mutiraoId);

    RelatorioExcelFile exportarExcelPorMutirao(Long mutiraoId);
}
