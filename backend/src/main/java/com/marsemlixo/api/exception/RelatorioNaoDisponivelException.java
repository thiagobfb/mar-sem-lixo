package com.marsemlixo.api.exception;

import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

public class RelatorioNaoDisponivelException extends RuntimeException {

    public RelatorioNaoDisponivelException(Long mutiraoId, MutiraoStatus status) {
        super("Relatório final indisponível para o mutirão " + mutiraoId
                + " com status " + status + ". Exporte apenas mutirões CONCLUIDO.");
    }
}
