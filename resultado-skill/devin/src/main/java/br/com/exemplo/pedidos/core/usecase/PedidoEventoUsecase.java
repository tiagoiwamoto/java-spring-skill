package br.com.exemplo.pedidos.core.usecase;

import java.time.Instant;

import br.com.exemplo.pedidos.adapter.KafkaAdapterOut;
import br.com.exemplo.pedidos.avro.PedidoEvento;
import br.com.exemplo.pedidos.core.entity.EventoPedido;
import br.com.exemplo.pedidos.core.error.PedidoEventoSerializationException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PedidoEventoUsecase {

    private final KafkaAdapterOut kafkaAdapterOut;
    private final ObjectMapper objectMapper;
    private final String topicoEventos;

    public PedidoEventoUsecase(
            KafkaAdapterOut kafkaAdapterOut,
            ObjectMapper objectMapper,
            @Value("${pedidos.kafka.topic.eventos}") String topicoEventos) {
        this.kafkaAdapterOut = kafkaAdapterOut;
        this.objectMapper = objectMapper;
        this.topicoEventos = topicoEventos;
    }

    public RecordMetadata publicarComoTexto(String pedidoId, String tipoEvento, String detalhe) {
        EventoPedido evento = novoEvento(pedidoId, tipoEvento, detalhe);
        return kafkaAdapterOut.publishPlainText(topicoEventos, pedidoId, toJson(evento));
    }

    public RecordMetadata publicarComoAvro(String pedidoId, String tipoEvento, String detalhe) {
        EventoPedido evento = novoEvento(pedidoId, tipoEvento, detalhe);
        PedidoEvento registro = PedidoEvento.newBuilder()
                .setPedidoId(evento.pedidoId())
                .setTipoEvento(evento.tipoEvento())
                .setDetalhe(evento.detalhe())
                .setOcorridoEm(evento.ocorridoEm().toString())
                .build();
        return kafkaAdapterOut.publishWithSslAndSchemaRegistry(topicoEventos, pedidoId, registro);
    }

    private EventoPedido novoEvento(String pedidoId, String tipoEvento, String detalhe) {
        return new EventoPedido(pedidoId, tipoEvento, detalhe, Instant.now());
    }

    private String toJson(EventoPedido evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JacksonException e) {
            throw new PedidoEventoSerializationException("falha ao serializar evento de pedido " + evento.pedidoId(), e);
        }
    }
}
