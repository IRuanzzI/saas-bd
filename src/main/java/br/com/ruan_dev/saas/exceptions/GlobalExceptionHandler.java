package br.com.ruan_dev.saas.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte exceções em respostas JSON {"status": 400, "message": "..."}.
 * O app usa o campo "message" para avisar o usuário.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErroResponse(int status, String message) {
    }

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), mensagem));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciais(CredenciaisInvalidasException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AcessoBloqueadoException.class)
    public ResponseEntity<ErroResponse> bloqueado(AcessoBloqueadoException e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ErroResponse> limite(LimiteExcedidoException e) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> indisponivel(ServicoIndisponivelException e) {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> negocio(NegocioException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getDefaultMessage())
                .orElse("Dados inválidos.");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    /**
     * Violação de integridade do banco:
     * - 23505 (valor único repetido): ex.: e-mail já cadastrado;
     * - qualquer outra (ex.: 23503, chave estrangeira): ex.: excluir cliente
     *   que ainda tem agendamentos.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e) {
        Throwable causa = e.getMostSpecificCause();
        String estado = causa instanceof java.sql.SQLException sql ? sql.getSQLState() : null;
        if ("23505".equals(estado)) {
            return resposta(HttpStatus.CONFLICT,
                    "Já existe um registro com estes dados (por exemplo, um e-mail já cadastrado).");
        }
        return resposta(HttpStatus.CONFLICT,
                "Este registro está em uso e não pode ser removido.");
    }
}
