package br.com.ruan_dev.saas.exceptions;

/** Serviço externo fora do ar (ex.: Evolution API): vira HTTP 503 com a mensagem. */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String message) {
        super(message);
    }
}
