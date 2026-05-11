package com.marsemlixo.api.exception;

import com.marsemlixo.api.mutirao.domain.MutiraoStatus;

public class MutiraoNaoEditavelException extends RuntimeException {
    public MutiraoNaoEditavelException(MutiraoStatus status) {
        super("Mutirão com status " + status + " não pode ser editado; somente PLANEJADO permite edição");
    }
}
