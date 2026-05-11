package com.marsemlixo.api.exception;

import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

public class TransicaoStatusInvalidaException extends RuntimeException {
    public TransicaoStatusInvalidaException(MutiraoStatus atual, MutiraoStatus novo) {
        super("Transição de status inválida: " + atual + " → " + novo);
    }

    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
