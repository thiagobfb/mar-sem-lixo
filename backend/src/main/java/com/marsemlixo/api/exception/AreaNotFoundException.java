package com.marsemlixo.api.exception;

public class AreaNotFoundException extends RuntimeException {
    public AreaNotFoundException(Long id) {
        super("Área não encontrada: " + id);
    }
}
