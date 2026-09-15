package br.com.exemplo.pedidos.entrypoint.rest.dto;

public record PedidoEventoResponse(
        String pedidoId,
        String topic,
        int partition,
        long offset
) {
}
