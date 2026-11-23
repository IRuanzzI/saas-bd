package br.com.ruan_dev.saas.exceptions;

/** Muitas tentativas (ex.: login): vira HTTP 429 com a mensagem. */
public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String message) {
        super(message);
    }
}
