package br.com.exemplo.pedidos.entrypoint.rest.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String message, String path, List<String> violations) {
}
