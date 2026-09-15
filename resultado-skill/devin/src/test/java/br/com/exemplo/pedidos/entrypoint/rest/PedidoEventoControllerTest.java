package br.com.exemplo.pedidos.entrypoint.rest;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import br.com.exemplo.pedidos.core.usecase.PedidoEventoUsecase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoEventoController.class)
class PedidoEventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoEventoUsecase usecase;

    @Test
    void postValidoRetorna202ComMetadata() throws Exception {
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.topic()).thenReturn("pedidos-eventos");
        when(metadata.partition()).thenReturn(1);
        when(metadata.offset()).thenReturn(10L);
        when(usecase.publicarComoTexto(eq("ped-1"), eq("CRIADO"), any())).thenReturn(metadata);

        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":\"ped-1\",\"tipoEvento\":\"CRIADO\",\"detalhe\":\"d\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pedidoId").value("ped-1"))
                .andExpect(jsonPath("$.topic").value("pedidos-eventos"))
                .andExpect(jsonPath("$.partition").value(1))
                .andExpect(jsonPath("$.offset").value(10));
    }

    @Test
    void postInvalidoRetorna400ComViolacoes() throws Exception {
        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":\"\",\"tipoEvento\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/pedidos/eventos"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    void falhaDePublicacaoRetorna502() throws Exception {
        when(usecase.publicarComoTexto(any(), any(), any()))
                .thenThrow(new KafkaPublishException("falha ao publicar", new RuntimeException("broker down")));

        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":\"ped-1\",\"tipoEvento\":\"CRIADO\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("falha ao publicar evento no Kafka"));
    }
}
