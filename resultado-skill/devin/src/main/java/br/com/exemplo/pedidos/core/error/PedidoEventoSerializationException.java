package br.com.exemplo.pedidos.core.error;

public class PedidoEventoSerializationException extends RuntimeException {

    public PedidoEventoSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
