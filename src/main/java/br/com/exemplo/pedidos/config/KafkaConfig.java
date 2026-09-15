package br.com.exemplo.pedidos.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.plain.acks:all}")
    private String plainAcks;

    @Value("${kafka.plain.retries:3}")
    private Integer plainRetries;

    @Value("${kafka.plain.delivery.timeout.ms:120000}")
    private Integer plainDeliveryTimeoutMs;

    @Value("${kafka.ssl.bootstrap.servers:localhost:9093}")
    private String sslBootstrapServers;

    @Value("${kafka.ssl.schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${kafka.ssl.truststore.location:#{null}}")
    private String sslTruststoreLocation;

    @Value("${kafka.ssl.truststore.password:}")
    private String sslTruststorePassword;

    @Value("${kafka.ssl.endpoint.identification.algorithm:https}")
    private String sslEndpointIdentificationAlgorithm;

    @Value("${kafka.ssl.acks:all}")
    private String sslAcks;

    @Value("${kafka.ssl.retries:3}")
    private Integer sslRetries;

    @Bean
    @Qualifier("kafkaPlainTextProducer")
    public Producer<String, String> kafkaPlainTextProducer() {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, plainAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, plainRetries);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, plainDeliveryTimeoutMs);

        var producer = new KafkaProducer<String, String>(props);
        log.info("Kafka producer de texto puro criado. Bootstrap: {}", bootstrapServers);
        return producer;
    }

    @Bean
    @Qualifier("kafkaSslSchemaRegistryProducer")
    public Producer<String, Object> kafkaSslSchemaRegistryProducer() {
        var truststoreLocation = resolveTruststoreLocation();

        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, sslAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, sslRetries);
        props.put("security.protocol", "SSL");
        props.put("ssl.truststore.location", truststoreLocation);
        props.put("ssl.truststore.password", sslTruststorePassword);
        props.put("ssl.endpoint.identification.algorithm", sslEndpointIdentificationAlgorithm);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("auto.register.schemas", false);
        props.put("use.latest.version", true);

        var producer = new KafkaProducer<String, Object>(props);
        log.info("Kafka producer SSL + Schema Registry criado. Bootstrap: {}, SchemaRegistry: {}, Truststore: {}",
                sslBootstrapServers, schemaRegistryUrl, truststoreLocation);
        return producer;
    }

    private String resolveTruststoreLocation() {
        if (sslTruststoreLocation != null && !sslTruststoreLocation.isBlank()) {
            return sslTruststoreLocation;
        }
        var javaHome = System.getProperty("java.home");
        return javaHome + "/lib/security/cacerts";
    }
}
