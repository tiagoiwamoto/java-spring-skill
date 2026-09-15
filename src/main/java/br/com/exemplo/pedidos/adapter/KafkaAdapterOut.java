package br.com.exemplo.pedidos.adapter;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class KafkaAdapterOut {

    private final Producer<String, String> kafkaPlainTextProducer;
    private final Producer<String, Object> kafkaSslSchemaRegistryProducer;

    public KafkaAdapterOut(
            @Qualifier("kafkaPlainTextProducer") Producer<String, String> kafkaPlainTextProducer,
            @Qualifier("kafkaSslSchemaRegistryProducer") Producer<String, Object> kafkaSslSchemaRegistryProducer
    ) {
        this.kafkaPlainTextProducer = kafkaPlainTextProducer;
        this.kafkaSslSchemaRegistryProducer = kafkaSslSchemaRegistryProducer;
    }

    public RecordMetadata publishPlainText(final String topic, final String message) {
        return publishPlainText(topic, null, message);
    }

    public RecordMetadata publishPlainText(final String topic, final String key, final String message) {
        validateTopic(topic);
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        var record = new ProducerRecord<String, String>(topic, key, message);
        try {
            var metadata = kafkaPlainTextProducer.send(record).get();
            log.info("Mensagem publicada no Kafka (texto puro). Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);
            return metadata;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Publicação Kafka (texto puro) interrompida", e);
        } catch (ExecutionException e) {
            throw new KafkaPublishException("Falha ao publicar no Kafka (texto puro)", e.getCause());
        }
    }

    public RecordMetadata publishWithSslAndSchemaRegistry(final String topic, final Object payload) {
        return publishWithSslAndSchemaRegistry(topic, null, payload);
    }

    public RecordMetadata publishWithSslAndSchemaRegistry(final String topic, final String key, final Object payload) {
        validateTopic(topic);
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }

        var record = new ProducerRecord<String, Object>(topic, key, payload);
        try {
            var metadata = kafkaSslSchemaRegistryProducer.send(record).get();
            log.info("Mensagem publicada no Kafka (SSL + Schema Registry). Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);
            return metadata;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Publicação Kafka (SSL + Schema Registry) interrompida", e);
        } catch (ExecutionException e) {
            throw new KafkaPublishException("Falha ao publicar no Kafka (SSL + Schema Registry)", e.getCause());
        }
    }

    private void validateTopic(final String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
    }
}
