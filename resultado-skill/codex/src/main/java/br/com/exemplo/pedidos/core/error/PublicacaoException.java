package br.com.exemplo.pedidos.core.error;
public class PublicacaoException extends RuntimeException {
    public PublicacaoException(String topic, Throwable cause) { super("Falha ao publicar no tópico " + topic, cause); }
}
