package br.com.exemplo.pedidos.config;

import java.util.Properties;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean(name = "kafkaPlainTextProducer", destroyMethod = "close")
    public Producer<String, String> kafkaPlainTextProducer(
            @Value("${kafka.bootstrap.servers}") String bootstrapServers,
            @Value("${kafka.plain.acks}") String acks,
            @Value("${kafka.plain.retries}") int retries,
            @Value("${kafka.plain.delivery.timeout.ms}") int deliveryTimeoutMs) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        return new KafkaProducer<>(props);
    }

    @Bean(name = "kafkaSslSchemaRegistryProducer", destroyMethod = "close")
    public Producer<String, Object> kafkaSslSchemaRegistryProducer(
            @Value("${kafka.ssl.bootstrap.servers}") String bootstrapServers,
            @Value("${kafka.ssl.schema.registry.url}") String schemaRegistryUrl,
            @Value("${kafka.ssl.acks}") String acks,
            @Value("${kafka.ssl.retries}") int retries,
            @Value("${kafka.ssl.truststore.location}") String truststoreLocation,
            @Value("${kafka.ssl.truststore.password}") String truststorePassword,
            @Value("${kafka.ssl.endpoint.identification.algorithm}") String endpointIdentificationAlgorithm) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resolveTruststoreLocation(truststoreLocation));
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("auto.register.schemas", false);
        props.put("use.latest.version", true);
        return new KafkaProducer<>(props);
    }

    static String resolveTruststoreLocation(String configuredLocation) {
        if (configuredLocation == null || configuredLocation.isBlank()) {
            return System.getProperty("java.home") + "/lib/security/cacerts";
        }
        return configuredLocation;
    }
}
