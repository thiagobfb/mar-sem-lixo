package com.marsemlixo.api.exception;

import java.util.UUID;

public class AreaNotFoundException extends RuntimeException {
    public AreaNotFoundException(UUID id) {
        super("Área não encontrada: " + id);
    }
}
