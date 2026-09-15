package br.com.exemplo.pedidos.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    @Test
    void truststoreEmBrancoResolveParaCacertsDoJdk() {
        String esperado = System.getProperty("java.home") + "/lib/security/cacerts";
        assertThat(KafkaConfig.resolveTruststoreLocation("")).isEqualTo(esperado);
        assertThat(KafkaConfig.resolveTruststoreLocation("   ")).isEqualTo(esperado);
        assertThat(KafkaConfig.resolveTruststoreLocation(null)).isEqualTo(esperado);
    }

    @Test
    void truststoreConfiguradoEhPreservado() {
        assertThat(KafkaConfig.resolveTruststoreLocation("/caminho/truststore.jks"))
                .isEqualTo("/caminho/truststore.jks");
    }
}
