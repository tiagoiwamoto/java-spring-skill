package br.com.exemplo.pedidos.config;
import java.nio.file.Path;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import static org.junit.jupiter.api.Assertions.*;
class KafkaConfigTest {
    @Test void preservesModesAndSeparatesSecurity() throws Exception {
        var env = new MockEnvironment();
        env.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        env.setProperty("kafka.ssl.truststore.location", " ");
        env.setProperty("kafka.ssl.truststore.password", "broker-test");
        env.setProperty("kafka.ssl.schema.registry.ssl.truststore.password", "registry-test");
        var config = new KafkaConfig(env);
        var plain = config.plainProperties();
        assertEquals("PLAINTEXT", plain.get("security.protocol"));
        assertEquals(StringSerializer.class, plain.get("value.serializer"));
        assertEquals("all", plain.get("acks"));
        assertEquals("3", plain.get("retries"));
        assertEquals("120000", plain.get("delivery.timeout.ms"));
        assertFalse(plain.containsKey("schema.registry.url"));
        var avro = config.avroProperties();
        assertEquals("SSL", avro.get("security.protocol"));
        assertEquals(KafkaAvroSerializer.class, avro.get("value.serializer"));
        assertEquals(StringSerializer.class, avro.get("key.serializer"));
        assertEquals(false, avro.get("auto.register.schemas"));
        assertEquals(true, avro.get("use.latest.version"));
        assertEquals("https", avro.get("ssl.endpoint.identification.algorithm"));
        assertEquals(Path.of(System.getProperty("java.home"), "lib", "security", "cacerts").toString(), avro.get("ssl.truststore.location"));
        assertEquals("broker-test", avro.get("ssl.truststore.password"));
        assertEquals("registry-test", avro.get("schema.registry.ssl.truststore.password"));
    }
}
