package com.marsemlixo.api.exception;

public class NomeDuplicadoException extends RuntimeException {
    public NomeDuplicadoException(String nome, String municipio) {
        super("Já existe uma área com o nome '" + nome + "' no município '" + municipio + "'");
    }
}
