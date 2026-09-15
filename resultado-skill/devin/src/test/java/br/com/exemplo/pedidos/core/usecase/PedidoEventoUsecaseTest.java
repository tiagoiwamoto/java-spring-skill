package br.com.exemplo.pedidos.core.usecase;

import br.com.exemplo.pedidos.adapter.KafkaAdapterOut;
import br.com.exemplo.pedidos.avro.PedidoEvento;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoEventoUsecaseTest {

    private static final String TOPICO = "pedidos-eventos";

    @Mock
    private KafkaAdapterOut adapter;

    private PedidoEventoUsecase usecase;

    @BeforeEach
    void setUp() {
        usecase = new PedidoEventoUsecase(adapter, JsonMapper.builder().build(), TOPICO);
    }

    @Test
    void publicarComoTextoPublicaJsonNoTopicoConfiguradoComPedidoIdComoChave() {
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(adapter.publishPlainText(eq(TOPICO), eq("ped-1"), any())).thenReturn(metadata);

        RecordMetadata resultado = usecase.publicarComoTexto("ped-1", "CRIADO", "detalhe");

        assertThat(resultado).isSameAs(metadata);
        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(adapter).publishPlainText(eq(TOPICO), eq("ped-1"), captor.capture());
        String json = captor.getValue();
        assertThat(json).contains("\"pedidoId\":\"ped-1\"")
                .contains("\"tipoEvento\":\"CRIADO\"")
                .contains("\"detalhe\":\"detalhe\"")
                .contains("\"ocorridoEm\":");
    }

    @Test
    void publicarComoAvroPublicaRegistroAvroNoTopicoConfiguradoComPedidoIdComoChave() {
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(adapter.publishWithSslAndSchemaRegistry(eq(TOPICO), eq("ped-2"), any()))
                .thenReturn(metadata);

        RecordMetadata resultado = usecase.publicarComoAvro("ped-2", "ATUALIZADO", null);

        assertThat(resultado).isSameAs(metadata);
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(adapter).publishWithSslAndSchemaRegistry(eq(TOPICO), eq("ped-2"), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PedidoEvento.class);
        PedidoEvento registro = (PedidoEvento) captor.getValue();
        assertThat(registro.getPedidoId()).isEqualTo("ped-2");
        assertThat(registro.getTipoEvento()).isEqualTo("ATUALIZADO");
        assertThat(registro.getDetalhe()).isNull();
        assertThat(registro.getOcorridoEm()).isNotBlank();
    }
}
