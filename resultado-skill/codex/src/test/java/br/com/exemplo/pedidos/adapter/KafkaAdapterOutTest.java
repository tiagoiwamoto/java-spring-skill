package br.com.exemplo.pedidos.adapter;

import br.com.exemplo.pedidos.core.error.PublicacaoException;
import java.util.concurrent.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaAdapterOutTest {
    @SuppressWarnings("unchecked")
    private final Producer<String, String> plain = mock(Producer.class);
    @SuppressWarnings("unchecked")
    private final Producer<String, Object> avro = mock(Producer.class);
    private final KafkaAdapterOut adapter = new KafkaAdapterOut(plain, avro);
    private final Object record = PedidoAvro.criar("p-1", "CRIADO");
    private RecordMetadata publish(boolean ssl, String topic, String key) {
        return ssl ? adapter.publishWithSslAndSchemaRegistry(topic, key, record) : adapter.publishPlainText(topic, key, "texto original");
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void successWithAndWithoutKey(boolean ssl) {
        var metadata = new RecordMetadata(new TopicPartition("pedidos", 2), 10, 0, 0, 1, 1);
        when(plain.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));
        when(avro.send(any())).thenReturn(CompletableFuture.completedFuture(metadata));
        assertSame(metadata, publish(ssl, "pedidos", "p-1"));
        assertSame(metadata, ssl ? adapter.publishWithSslAndSchemaRegistry("pedidos", record) : adapter.publishPlainText("pedidos", "texto original"));
        if (ssl) {
            verify(avro).send(argThat(r -> r.topic().equals("pedidos") && "p-1".equals(r.key()) && r.value() == record));
            verify(avro).send(argThat(r -> r.key() == null && r.value() == record));
            verifyNoInteractions(plain);
        } else {
            verify(plain).send(argThat(r -> r.topic().equals("pedidos") && "p-1".equals(r.key()) && r.value().equals("texto original")));
            verify(plain).send(argThat(r -> r.key() == null && r.value().equals("texto original")));
            verifyNoInteractions(avro);
        }
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void invalidTopicAndPayloadNeverSend(boolean ssl) {
        for (String topic : new String[]{null, "", " ", "bad topic", ".", "..", "a".repeat(250)})
            assertThrows(IllegalArgumentException.class, () -> publish(ssl, topic, null));
        assertThrows(IllegalArgumentException.class, () -> {
            if (ssl) adapter.publishWithSslAndSchemaRegistry("pedidos", null);
            else adapter.publishPlainText("pedidos", null);
        });
        if (ssl) {
            assertThrows(IllegalArgumentException.class, () -> adapter.publishWithSslAndSchemaRegistry("pedidos", new Object()));
            var invalid = PedidoAvro.criar("1", "CRIADO");
            invalid.put("status", 42);
            assertThrows(IllegalArgumentException.class, () -> adapter.publishWithSslAndSchemaRegistry("pedidos", invalid));
        }
        verifyNoInteractions(plain, avro);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void preservesAsyncCause(boolean ssl) {
        var cause = new IllegalStateException("producer failure");
        when(plain.send(any())).thenReturn(CompletableFuture.failedFuture(cause));
        when(avro.send(any())).thenReturn(CompletableFuture.failedFuture(cause));
        assertSame(cause, assertThrows(PublicacaoException.class, () -> publish(ssl, "pedidos", null)).getCause());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void wrapsImmediateFailure(boolean ssl) {
        var cause = new IllegalStateException("producer closed");
        when(plain.send(any())).thenThrow(cause);
        when(avro.send(any())).thenThrow(cause);
        assertSame(cause, assertThrows(PublicacaoException.class, () -> publish(ssl, "pedidos", null)).getCause());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    @SuppressWarnings("unchecked")
    void restoresInterruption(boolean ssl) throws Exception {
        Future<RecordMetadata> future = mock(Future.class);
        var cause = new InterruptedException("interrupted");
        when(future.get()).thenThrow(cause);
        when(plain.send(any())).thenReturn(future);
        when(avro.send(any())).thenReturn(future);
        try {
            assertSame(cause, assertThrows(PublicacaoException.class, () -> publish(ssl, "pedidos", null)).getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
