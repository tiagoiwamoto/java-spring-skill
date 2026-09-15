package br.com.exemplo.pedidos.core.entity;

import java.time.Instant;

public record EventoPedido(String pedidoId, String tipoEvento, String detalhe, Instant ocorridoEm) {
}
