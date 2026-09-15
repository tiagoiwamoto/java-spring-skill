package br.com.exemplo.pedidos.adapter;

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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdapterOutTest {

    private static final String TOPICO = "pedidos-eventos";

    @Mock
    private Producer<String, String> plainTextProducer;

    @Mock
    private Producer<String, Object> sslSchemaRegistryProducer;

    private KafkaAdapterOut adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaAdapterOut(plainTextProducer, sslSchemaRegistryProducer);
    }

    @Test
    void publishPlainText_comChave_enviaRegistroERetornaMetadata() {
        var metadata = mock(RecordMetadata.class);
        when(plainTextProducer.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));

        var resultado = adapter.publishPlainText(TOPICO, "ped-1", "mensagem-de-evento");

        assertSame(metadata, resultado);

        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(plainTextProducer).send(captor.capture());
        var record = captor.getValue();
        assertEquals(TOPICO, record.topic());
        assertEquals("ped-1", record.key());
        assertEquals("mensagem-de-evento", record.value());
        verifyNoInteractions(sslSchemaRegistryProducer);
    }

    @Test
    void publishPlainText_semChave_enviaChaveNula() {
        var metadata = mock(RecordMetadata.class);
        when(plainTextProducer.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));

        var resultado = adapter.publishPlainText(TOPICO, "mensagem-de-evento");

        assertSame(metadata, resultado);

        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(plainTextProducer).send(captor.capture());
        assertNull(captor.getValue().key());
        assertEquals("mensagem-de-evento", captor.getValue().value());
    }

    @Test
    void publishPlainText_topicoInvalidoOuMensagemNula_rejeitaSemEnviar() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishPlainText(null, "mensagem"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishPlainText("   ", "mensagem"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishPlainText(TOPICO, null));

        verifyNoInteractions(plainTextProducer, sslSchemaRegistryProducer);
    }

    @Test
    void publishWithSslAndSchemaRegistry_comChave_enviaRegistroAvroERetornaMetadata() {
        var evento = PedidoEvento.newBuilder()
                .setPedidoId("ped-9")
                .setTipo("CRIADO")
                .setOcorridoEm(1_770_000_000_000L)
                .setDetalhe("canal=app")
                .build();

        var metadata = mock(RecordMetadata.class);
        when(sslSchemaRegistryProducer.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));

        var resultado = adapter.publishWithSslAndSchemaRegistry("pedidos-eventos-avro", "ped-9", evento);

        assertSame(metadata, resultado);

        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(sslSchemaRegistryProducer).send(captor.capture());
        var record = captor.getValue();
        assertEquals("pedidos-eventos-avro", record.topic());
        assertEquals("ped-9", record.key());
        assertSame(evento, record.value());
        verifyNoInteractions(plainTextProducer);
    }

    @Test
    void publishWithSslAndSchemaRegistry_semChave_enviaChaveNula() {
        var evento = PedidoEvento.newBuilder()
                .setPedidoId("ped-9")
                .setTipo("CANCELADO")
                .setOcorridoEm(1_770_000_000_000L)
                .build();

        when(sslSchemaRegistryProducer.send(any()))
                .thenReturn(CompletableFuture.completedFuture(mock(RecordMetadata.class)));

        adapter.publishWithSslAndSchemaRegistry("pedidos-eventos-avro", evento);

        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(sslSchemaRegistryProducer).send(captor.capture());
        assertNull(captor.getValue().key());
        assertSame(evento, captor.getValue().value());
    }

    @Test
    void publishWithSslAndSchemaRegistry_topicoInvalidoOuPayloadNulo_rejeitaSemEnviar() {
        var evento = PedidoEvento.newBuilder()
                .setPedidoId("ped-9")
                .setTipo("CRIADO")
                .setOcorridoEm(1_770_000_000_000L)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishWithSslAndSchemaRegistry(null, evento));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishWithSslAndSchemaRegistry("", evento));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishWithSslAndSchemaRegistry("pedidos-eventos-avro", null));

        verifyNoInteractions(plainTextProducer, sslSchemaRegistryProducer);
    }

    @Test
    void publishPlainText_falhaDoProducer_preservaCausaOriginal() {
        var causa = new RuntimeException("broker indisponível");
        var future = new CompletableFuture<RecordMetadata>();
        future.completeExceptionally(causa);
        when(plainTextProducer.send(any())).thenReturn(future);

        var exception = assertThrows(KafkaPublishException.class,
                () -> adapter.publishPlainText(TOPICO, "mensagem"));

        assertSame(causa, exception.getCause());
    }

    @Test
    void publishWithSslAndSchemaRegistry_falhaDoProducer_preservaCausaOriginal() {
        var causa = new RuntimeException("schema incompatível");
        var future = new CompletableFuture<RecordMetadata>();
        future.completeExceptionally(causa);
        when(sslSchemaRegistryProducer.send(any())).thenReturn(future);

        var evento = PedidoEvento.newBuilder()
                .setPedidoId("ped-9")
                .setTipo("CRIADO")
                .setOcorridoEm(1_770_000_000_000L)
                .build();

        var exception = assertThrows(KafkaPublishException.class,
                () -> adapter.publishWithSslAndSchemaRegistry("pedidos-eventos-avro", evento));

        assertSame(causa, exception.getCause());
    }

    @Test
    void publishPlainText_interrompido_restauraFlagDeInterrupcao() throws Exception {
        @SuppressWarnings("unchecked")
        Future<RecordMetadata> future = mock(Future.class);
        when(future.get()).thenThrow(new InterruptedException("interrompido"));
        when(plainTextProducer.send(any())).thenReturn(future);

        try {
            assertThrows(KafkaPublishException.class,
                    () -> adapter.publishPlainText(TOPICO, "mensagem"));

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void publishWithSslAndSchemaRegistry_interrompido_restauraFlagDeInterrupcao() throws Exception {
        @SuppressWarnings("unchecked")
        Future<RecordMetadata> future = mock(Future.class);
        when(future.get()).thenThrow(new InterruptedException("interrompido"));
        when(sslSchemaRegistryProducer.send(any())).thenReturn(future);

        var evento = PedidoEvento.newBuilder()
                .setPedidoId("ped-9")
                .setTipo("CRIADO")
                .setOcorridoEm(1_770_000_000_000L)
                .build();

        try {
            assertThrows(KafkaPublishException.class,
                    () -> adapter.publishWithSslAndSchemaRegistry("pedidos-eventos-avro", evento));

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
