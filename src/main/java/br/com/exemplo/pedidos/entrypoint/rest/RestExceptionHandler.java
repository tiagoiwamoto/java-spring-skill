package br.com.exemplo.pedidos.entrypoint.rest;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import br.com.exemplo.pedidos.core.error.PedidoEventoSerializationException;
import br.com.exemplo.pedidos.entrypoint.rest.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        var violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valor inválido",
                        (firstMessage, secondMessage) -> firstMessage + "; " + secondMessage,
                        LinkedHashMap::new
                ));

        return validationError(request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        var violations = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (firstMessage, secondMessage) -> firstMessage + "; " + secondMessage,
                        LinkedHashMap::new
                ));

        return validationError(request, violations);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        var status = HttpStatus.BAD_REQUEST;
        var error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(KafkaPublishException.class)
    public ResponseEntity<ApiError> handleKafkaPublishException(
            KafkaPublishException exception,
            HttpServletRequest request
    ) {
        log.error("Falha ao publicar evento no Kafka: path={}", request.getRequestURI(), exception);

        var status = HttpStatus.BAD_GATEWAY;
        var error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Falha ao publicar evento no Kafka",
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(PedidoEventoSerializationException.class)
    public ResponseEntity<ApiError> handlePedidoEventoSerializationException(
            PedidoEventoSerializationException exception,
            HttpServletRequest request
    ) {
        log.error("Falha ao serializar evento de pedido: path={}", request.getRequestURI(), exception);

        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Falha ao serializar evento de pedido",
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ApiError> validationError(
            HttpServletRequest request,
            Map<String, String> violations
    ) {
        var status = HttpStatus.BAD_REQUEST;
        var error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Um ou mais campos são inválidos",
                request.getRequestURI(),
                violations
        );

        return ResponseEntity.badRequest().body(error);
    }
}
