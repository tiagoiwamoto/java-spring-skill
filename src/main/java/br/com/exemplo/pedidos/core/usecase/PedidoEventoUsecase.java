package br.com.exemplo.pedidos.core.usecase;

import br.com.exemplo.pedidos.adapter.KafkaAdapterOut;
import br.com.exemplo.pedidos.core.error.PedidoEventoSerializationException;
import br.com.exemplo.pedidos.entrypoint.rest.dto.PedidoEventoRequest;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PedidoEventoUsecase {

    private final KafkaAdapterOut kafkaAdapterOut;
    private final ObjectMapper objectMapper;
    private final String topicEventos;

    public PedidoEventoUsecase(
            KafkaAdapterOut kafkaAdapterOut,
            ObjectMapper objectMapper,
            @Value("${pedidos.kafka.topic.eventos:pedidos-eventos}") String topicEventos
    ) {
        this.kafkaAdapterOut = kafkaAdapterOut;
        this.objectMapper = objectMapper;
        this.topicEventos = topicEventos;
    }

    public RecordMetadata publicarEventoTexto(PedidoEventoRequest request) {
        var mensagem = serializar(request);
        return kafkaAdapterOut.publishPlainText(topicEventos, request.pedidoId(), mensagem);
    }

    private String serializar(PedidoEventoRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException e) {
            throw new PedidoEventoSerializationException("Falha ao serializar evento de pedido para texto", e);
        }
    }
}
