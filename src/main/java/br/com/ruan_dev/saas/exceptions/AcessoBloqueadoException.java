package br.com.ruan_dev.saas.exceptions;

/** Empresa suspensa ou com assinatura vencida: vira HTTP 403 com a mensagem. */
public class AcessoBloqueadoException extends RuntimeException {
    public AcessoBloqueadoException(String message) {
        super(message);
    }
}
