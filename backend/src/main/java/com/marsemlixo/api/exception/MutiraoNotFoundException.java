package com.marsemlixo.api.exception;

public class MutiraoNotFoundException extends RuntimeException {
    public MutiraoNotFoundException(Long id) {
        super("Mutirão não encontrado: " + id);
    }
}
