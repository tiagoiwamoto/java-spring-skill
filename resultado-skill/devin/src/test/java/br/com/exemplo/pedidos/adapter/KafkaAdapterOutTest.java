package br.com.exemplo.pedidos.adapter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import br.com.exemplo.pedidos.avro.PedidoEvento;
import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdapterOutTest {

    @Mock
    private Producer<String, String> plainTextProducer;

    @Mock
    private Producer<String, Object> sslSchemaRegistryProducer;

    @Mock
    private Future<RecordMetadata> future;

    private KafkaAdapterOut adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaAdapterOut(plainTextProducer, sslSchemaRegistryProducer);
    }

    private RecordMetadata metadata() {
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.topic()).thenReturn("pedidos-eventos");
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(42L);
        return metadata;
    }

    @Test
    void publishPlainTextSemChaveDelegaComKeyNulaERetornaMetadata() throws Exception {
        RecordMetadata metadata = metadata();
        when(plainTextProducer.send(any())).thenReturn(future);
        when(future.get()).thenReturn(metadata);

        RecordMetadata resultado = adapter.publishPlainText("pedidos-eventos", "mensagem");

        assertThat(resultado).isSameAs(metadata);
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(plainTextProducer).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("pedidos-eventos");
        assertThat(captor.getValue().key()).isNull();
        assertThat(captor.getValue().value()).isEqualTo("mensagem");
    }

    @Test
    void publishPlainTextComChaveEnviaChaveERetornaMetadata() throws Exception {
        RecordMetadata metadata = metadata();
        when(plainTextProducer.send(any())).thenReturn(future);
        when(future.get()).thenReturn(metadata);

        RecordMetadata resultado = adapter.publishPlainText("pedidos-eventos", "ped-1", "mensagem");

        assertThat(resultado).isSameAs(metadata);
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(plainTextProducer).send(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("ped-1");
        assertThat(captor.getValue().value()).isEqualTo("mensagem");
    }

    @Test
    void publishWithSslAndSchemaRegistryComChaveUsaProducerSslERetornaMetadata() throws Exception {
        PedidoEvento payload = PedidoEvento.newBuilder()
                .setPedidoId("ped-1")
                .setTipoEvento("CRIADO")
                .setDetalhe("detalhe")
                .setOcorridoEm("2026-09-14T00:00:00Z")
                .build();
        RecordMetadata metadata = metadata();
        when(sslSchemaRegistryProducer.send(any())).thenReturn(future);
        when(future.get()).thenReturn(metadata);

        RecordMetadata resultado = adapter.publishWithSslAndSchemaRegistry("pedidos-eventos", "ped-1", payload);

        assertThat(resultado).isSameAs(metadata);
        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(sslSchemaRegistryProducer).send(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("ped-1");
        assertThat(captor.getValue().value()).isSameAs(payload);
        verify(plainTextProducer, never()).send(any());
    }

    @Test
    void publishWithSslAndSchemaRegistrySemChaveDelegaComKeyNula() throws Exception {
        PedidoEvento payload = PedidoEvento.newBuilder()
                .setPedidoId("ped-2")
                .setTipoEvento("ATUALIZADO")
                .setOcorridoEm("2026-09-14T00:00:00Z")
                .build();
        RecordMetadata metadata = metadata();
        when(sslSchemaRegistryProducer.send(any())).thenReturn(future);
        when(future.get()).thenReturn(metadata);

        adapter.publishWithSslAndSchemaRegistry("pedidos-eventos", payload);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(sslSchemaRegistryProducer).send(captor.capture());
        assertThat(captor.getValue().key()).isNull();
        assertThat(captor.getValue().value()).isSameAs(payload);
    }

    @Test
    void topicoNuloRejeitaSemEnviar() {
        assertThatThrownBy(() -> adapter.publishPlainText(null, "mensagem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topico");
        verify(plainTextProducer, never()).send(any());
    }

    @Test
    void topicoEmBrancoRejeitaSemEnviar() {
        assertThatThrownBy(() -> adapter.publishWithSslAndSchemaRegistry("   ", "chave", new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topico");
        verify(sslSchemaRegistryProducer, never()).send(any());
    }

    @Test
    void payloadNuloRejeitaSemEnviar() {
        assertThatThrownBy(() -> adapter.publishPlainText("pedidos-eventos", (String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload");
        assertThatThrownBy(() -> adapter.publishWithSslAndSchemaRegistry("pedidos-eventos", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload");
        verify(plainTextProducer, never()).send(any());
        verify(sslSchemaRegistryProducer, never()).send(any());
    }

    @Test
    void falhaDoProducerPropagaKafkaPublishExceptionPreservandoCausa() throws Exception {
        RuntimeException causa = new RuntimeException("broker indisponivel");
        when(plainTextProducer.send(any())).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException(causa));

        assertThatThrownBy(() -> adapter.publishPlainText("pedidos-eventos", "mensagem"))
                .isInstanceOf(KafkaPublishException.class)
                .hasMessageContaining("pedidos-eventos")
                .satisfies(ex -> assertThat(ex.getCause()).isSameAs(causa));
    }

    @Test
    void interrupcaoRestauraFlagDaThreadEPropaga() throws Exception {
        when(plainTextProducer.send(any())).thenReturn(future);
        when(future.get()).thenThrow(new InterruptedException("interrompido"));

        try {
            assertThatThrownBy(() -> adapter.publishPlainText("pedidos-eventos", "mensagem"))
                    .isInstanceOf(KafkaPublishException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.interrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
