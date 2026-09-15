package br.com.exemplo.pedidos.entrypoint.rest;
import br.com.exemplo.pedidos.adapter.KafkaAdapterOut;
import br.com.exemplo.pedidos.core.error.PublicacaoException;
import br.com.exemplo.pedidos.core.usecase.PublicarEventoPedido;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PedidoEventoControllerTest {
    private final KafkaAdapterOut adapter = mock(KafkaAdapterOut.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PedidoEventoController(
            new PublicarEventoPedido(adapter, new MockEnvironment().withProperty("pedidos.kafka.topic", "pedidos"))))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void confirmsPublicationAndPreservesText() throws Exception {
        when(adapter.publishPlainText("pedidos", "p-1", "texto original")).thenReturn(
                new RecordMetadata(new TopicPartition("pedidos", 0), 7, 0, 0, 1, 1));
        mvc.perform(post("/pedidos/eventos").contentType("application/json")
            .content("{\"chave\":\"p-1\",\"mensagem\":\"texto original\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.offset").value(7));
        verify(adapter).publishPlainText("pedidos", "p-1", "texto original");
    }
    @Test void missingPayloadIsBadRequest() throws Exception {
        mvc.perform(post("/pedidos/eventos").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.violacoes[0]").exists());
        verifyNoInteractions(adapter);
    }
    @Test void producerFailureBecomesBadGateway() throws Exception {
        when(adapter.publishPlainText("pedidos", null, "x")).thenThrow(new PublicacaoException("pedidos", new RuntimeException()));
        mvc.perform(post("/pedidos/eventos").contentType("application/json").content("{\"mensagem\":\"x\"}"))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.caminho").value("/pedidos/eventos"));
    }
}
