package br.com.ruan_dev.saas.exceptions;

/** Regra de negócio violada ou dado inválido (vira HTTP 400 com a mensagem). */
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}
