package br.com.exemplo.pedidos.adapter;

import java.util.concurrent.ExecutionException;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class KafkaAdapterOut {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdapterOut.class);

    private final Producer<String, String> plainTextProducer;
    private final Producer<String, Object> sslSchemaRegistryProducer;

    public KafkaAdapterOut(
            @Qualifier("kafkaPlainTextProducer") Producer<String, String> plainTextProducer,
            @Qualifier("kafkaSslSchemaRegistryProducer") Producer<String, Object> sslSchemaRegistryProducer) {
        this.plainTextProducer = plainTextProducer;
        this.sslSchemaRegistryProducer = sslSchemaRegistryProducer;
    }

    public RecordMetadata publishPlainText(String topic, String message) {
        return publishPlainText(topic, null, message);
    }

    public RecordMetadata publishPlainText(String topic, String key, String message) {
        return send(plainTextProducer, topic, key, message);
    }

    public RecordMetadata publishWithSslAndSchemaRegistry(String topic, Object payload) {
        return publishWithSslAndSchemaRegistry(topic, null, payload);
    }

    public RecordMetadata publishWithSslAndSchemaRegistry(String topic, String key, Object payload) {
        return send(sslSchemaRegistryProducer, topic, key, payload);
    }

    private <V> RecordMetadata send(Producer<String, V> producer, String topic, String key, V value) {
        validate(topic, value);
        ProducerRecord<String, V> record = new ProducerRecord<>(topic, key, value);
        try {
            RecordMetadata metadata = producer.send(record).get();
            log.info("mensagem publicada topic={} partition={} offset={} key={}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);
            return metadata;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("publicacao interrompida no topico " + topic, e);
        } catch (ExecutionException e) {
            throw new KafkaPublishException("falha ao publicar no topico " + topic, e.getCause());
        }
    }

    private void validate(String topic, Object value) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topico nao pode ser nulo ou em branco");
        }
        if (value == null) {
            throw new IllegalArgumentException("payload nao pode ser nulo");
        }
    }
}
