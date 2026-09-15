package br.com.exemplo.pedidos.entrypoint.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PedidoEventoRequest(
        @NotBlank String pedidoId,
        @NotBlank String tipoEvento,
        String detalhe) {
}
