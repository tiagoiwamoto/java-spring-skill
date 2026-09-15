package br.com.exemplo.pedidos.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class KafkaConfig {
    private final Environment env;
    @Bean(name = "kafkaPlainTextProducer", destroyMethod = "close")
    public Producer<String, String> plainProducer() { return new KafkaProducer<>(plainProperties()); }
    @Bean(name = "kafkaSslSchemaRegistryProducer", destroyMethod = "close")
    public Producer<String, Object> avroProducer() { return new KafkaProducer<>(avroProperties()); }
    Map<String, Object> plainProperties() {
        var p = base("kafka.plain", env.getRequiredProperty("kafka.bootstrap.servers"));
        p.put("security.protocol", "PLAINTEXT");
        p.put("value.serializer", StringSerializer.class);
        return p;
    }
    Map<String, Object> avroProperties() {
        var p = base("kafka.ssl", env.getRequiredProperty("kafka.ssl.bootstrap.servers"));
        p.put("value.serializer", KafkaAvroSerializer.class);
        p.put("security.protocol", "SSL");
        p.put("schema.registry.url", env.getRequiredProperty("kafka.ssl.schema.registry.url"));
        String truststore = env.getProperty("kafka.ssl.truststore.location", "");
        p.put("ssl.truststore.location", truststore.isBlank() ? Path.of(System.getProperty("java.home"), "lib", "security", "cacerts").toString() : truststore);
        p.put("ssl.endpoint.identification.algorithm", env.getRequiredProperty("kafka.ssl.endpoint.identification.algorithm"));
        p.put("auto.register.schemas", false);
        p.put("use.latest.version", true);
        for (String key : new String[]{"truststore.password", "truststore.type", "keystore.location", "keystore.password", "keystore.type", "key.password"}) {
            optional(p, "ssl." + key, "kafka.ssl." + key);
        }
        for (String key : new String[]{"ssl.truststore.location", "ssl.truststore.password", "ssl.truststore.type", "ssl.keystore.location", "ssl.keystore.password", "ssl.key.password", "basic.auth.credentials.source", "basic.auth.user.info"}) {
            optional(p, "schema.registry." + key, "kafka.ssl.schema.registry." + key);
        }
        return p;
    }
    private Map<String, Object> base(String prefix, String bootstrap) {
        Map<String, Object> p = new HashMap<>();
        p.put("bootstrap.servers", bootstrap);
        p.put("key.serializer", StringSerializer.class);
        for (String key : new String[]{"acks", "retries", "delivery.timeout.ms", "request.timeout.ms", "max.block.ms"})
            p.put(key, env.getRequiredProperty(prefix + "." + key));
        return p;
    }
    private void optional(Map<String, Object> p, String target, String source) {
        String value = env.getProperty(source);
        if (value != null && !value.isBlank()) p.put(target, value);
    }
}
