package br.com.exemplo.pedidos.entrypoint.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PedidoEventoRequest(
        @NotBlank(message = "pedidoId é obrigatório") String pedidoId,
        @NotBlank(message = "tipo é obrigatório") String tipo,
        String detalhe
) {
}
