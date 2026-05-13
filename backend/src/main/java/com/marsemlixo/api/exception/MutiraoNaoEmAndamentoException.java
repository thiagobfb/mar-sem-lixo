package com.marsemlixo.api.exception;

import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

public class MutiraoNaoEmAndamentoException extends RuntimeException {

    public MutiraoNaoEmAndamentoException(Long mutiraoId, MutiraoStatus statusAtual) {
        super("Mutirão " + mutiraoId + " não está EM_ANDAMENTO. Status atual: " + statusAtual);
    }
}
