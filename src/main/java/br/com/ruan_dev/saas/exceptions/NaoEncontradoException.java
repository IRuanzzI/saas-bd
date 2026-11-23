package br.com.ruan_dev.saas.exceptions;

/** Registro inexistente (ou de outra empresa): vira HTTP 404 com a mensagem. */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String message) {
        super(message);
    }
}
