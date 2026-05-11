package com.marsemlixo.api.exception;

public class AreaInativaException extends RuntimeException {
    public AreaInativaException(Long areaId) {
        super("Área não encontrada ou inativa: " + areaId);
    }
}
