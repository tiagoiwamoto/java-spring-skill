package br.com.exemplo.pedidos.entrypoint.rest;
import br.com.exemplo.pedidos.core.error.PublicacaoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(Instant instante, int status, String mensagem, String caminho, List<String> violacoes) {}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        return error(400, "Entrada inválida", request, e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList());
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception e, HttpServletRequest request) {
        return error(400, "Entrada inválida", request, List.of());
    }
    @ExceptionHandler(PublicacaoException.class)
    ResponseEntity<ApiError> integration(PublicacaoException e, HttpServletRequest request) {
        return error(502, "Não foi possível confirmar a publicação", request, List.of());
    }
    private ResponseEntity<ApiError> error(int status, String message, HttpServletRequest request, List<String> violations) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status, message, request.getRequestURI(), violations));
    }
}
