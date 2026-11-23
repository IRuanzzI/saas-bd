package br.com.ruan_dev.saas.exceptions;

/** E-mail ou senha incorretos no login: vira HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("Credenciais inválidas");
    }
}
