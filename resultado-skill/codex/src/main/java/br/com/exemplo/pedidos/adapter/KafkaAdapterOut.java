package br.com.exemplo.pedidos.adapter;

import br.com.exemplo.pedidos.core.error.PublicacaoException;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaAdapterOut {
    private final Producer<String, String> plain;
    private final Producer<String, Object> avro;
    public KafkaAdapterOut(@Qualifier("kafkaPlainTextProducer") Producer<String, String> plain,
            @Qualifier("kafkaSslSchemaRegistryProducer") Producer<String, Object> avro) {
        this.plain = plain;
        this.avro = avro;
    }
    public RecordMetadata publishPlainText(String topic, String message) { return publishPlainText(topic, null, message); }
    public RecordMetadata publishPlainText(String topic, String key, String message) {
        return send(plain, topic, key, message);
    }
    public RecordMetadata publishWithSslAndSchemaRegistry(String topic, Object payload) {
        return publishWithSslAndSchemaRegistry(topic, null, payload);
    }
    public RecordMetadata publishWithSslAndSchemaRegistry(String topic, String key, Object payload) {
        if (!(payload instanceof IndexedRecord record) || !GenericData.get().validate(record.getSchema(), record)) {
            throw new IllegalArgumentException("Payload deve ser um registro Avro válido");
        }
        return send(avro, topic, key, payload);
    }
    private <T> RecordMetadata send(Producer<String, T> producer, String topic, String key, T payload) {
        if (topic == null || topic.isBlank() || topic.length() > 249 || topic.equals(".") || topic.equals("..")
                || !topic.matches("[a-zA-Z0-9._-]+")) throw new IllegalArgumentException("Tópico inválido");
        if (payload == null) throw new IllegalArgumentException("Payload obrigatório");
        try {
            var metadata = producer.send(new ProducerRecord<>(topic, key, payload)).get();
            log.info("Evento publicado: tópico={}, partição={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            return metadata;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacaoException(topic, e);
        } catch (ExecutionException e) {
            throw new PublicacaoException(topic, e.getCause());
        } catch (RuntimeException e) {
            throw new PublicacaoException(topic, e);
        }
    }
}
