package com.marsemlixo.api.exception;

public class PoligonoInvalidoException extends RuntimeException {
    public PoligonoInvalidoException(String motivo) {
        super("Polígono inválido: " + motivo);
    }
}
