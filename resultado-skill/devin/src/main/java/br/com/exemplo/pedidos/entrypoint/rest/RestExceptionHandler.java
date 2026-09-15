package br.com.exemplo.pedidos.entrypoint.rest;

import java.time.Instant;
import java.util.List;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import br.com.exemplo.pedidos.core.error.PedidoEventoSerializationException;
import br.com.exemplo.pedidos.entrypoint.rest.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "requisicao invalida", request, violations);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(KafkaPublishException.class)
    public ResponseEntity<ApiError> handleKafkaPublish(KafkaPublishException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "falha ao publicar evento no Kafka", request, List.of());
    }

    @ExceptionHandler(PedidoEventoSerializationException.class)
    public ResponseEntity<ApiError> handleSerialization(PedidoEventoSerializationException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "falha ao serializar evento de pedido", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<String> violations) {
        ApiError body = new ApiError(Instant.now(), status.value(), message, request.getRequestURI(), violations);
        return ResponseEntity.status(status).body(body);
    }
}
