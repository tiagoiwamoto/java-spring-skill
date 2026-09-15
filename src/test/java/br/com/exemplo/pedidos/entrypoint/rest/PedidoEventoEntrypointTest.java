package br.com.exemplo.pedidos.entrypoint.rest;

import br.com.exemplo.pedidos.core.error.KafkaPublishException;
import br.com.exemplo.pedidos.core.usecase.PedidoEventoUsecase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoEventoEntrypoint.class)
class PedidoEventoEntrypointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoEventoUsecase usecase;

    @Test
    void postEventos_valido_publicaERetorna202() throws Exception {
        var metadata = mock(RecordMetadata.class);
        when(metadata.topic()).thenReturn("pedidos-eventos");
        when(metadata.partition()).thenReturn(2);
        when(metadata.offset()).thenReturn(42L);
        when(usecase.publicarEventoTexto(any())).thenReturn(metadata);

        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"pedidoId": "ped-1", "tipo": "CRIADO", "detalhe": "canal=app"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pedidoId").value("ped-1"))
                .andExpect(jsonPath("$.topic").value("pedidos-eventos"))
                .andExpect(jsonPath("$.partition").value(2))
                .andExpect(jsonPath("$.offset").value(42));
    }

    @Test
    void postEventos_semPedidoId_retorna400ComViolacoes() throws Exception {
        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"tipo": "CRIADO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.pedidoId").exists());
    }

    @Test
    void postEventos_falhaNaPublicacao_retorna502() throws Exception {
        when(usecase.publicarEventoTexto(any()))
                .thenThrow(new KafkaPublishException("Falha ao publicar no Kafka (texto puro)",
                        new RuntimeException("broker indisponível")));

        mockMvc.perform(post("/pedidos/eventos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"pedidoId": "ped-1", "tipo": "CRIADO"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }
}
