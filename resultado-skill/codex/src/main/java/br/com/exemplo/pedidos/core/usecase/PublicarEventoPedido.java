package br.com.exemplo.pedidos.core.usecase;
import br.com.exemplo.pedidos.adapter.KafkaAdapterOut;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
@Service
@RequiredArgsConstructor
public class PublicarEventoPedido {
    private final KafkaAdapterOut adapter;
    private final Environment environment;
    public RecordMetadata executar(String chave, String mensagem) {
        return adapter.publishPlainText(environment.getRequiredProperty("pedidos.kafka.topic"), chave, mensagem);
    }
}
