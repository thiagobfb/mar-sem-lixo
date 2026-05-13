package com.marsemlixo.api.exception;

import java.util.UUID;

public class RegistroResiduoNotFoundException extends RuntimeException {

    public RegistroResiduoNotFoundException(UUID id) {
        super("Registro de resíduo não encontrado: " + id);
    }
}
