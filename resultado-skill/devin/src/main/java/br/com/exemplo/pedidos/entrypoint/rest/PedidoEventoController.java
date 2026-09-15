package br.com.exemplo.pedidos.entrypoint.rest;

import br.com.exemplo.pedidos.core.usecase.PedidoEventoUsecase;
import br.com.exemplo.pedidos.entrypoint.rest.dto.PedidoEventoRequest;
import br.com.exemplo.pedidos.entrypoint.rest.dto.PedidoEventoResponse;
import jakarta.validation.Valid;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoEventoController {

    private final PedidoEventoUsecase usecase;

    public PedidoEventoController(PedidoEventoUsecase usecase) {
        this.usecase = usecase;
    }

    @PostMapping("/eventos")
    public ResponseEntity<PedidoEventoResponse> publicar(@Valid @RequestBody PedidoEventoRequest request) {
        RecordMetadata metadata = usecase.publicarComoTexto(request.pedidoId(), request.tipoEvento(), request.detalhe());
        PedidoEventoResponse body = new PedidoEventoResponse(
                request.pedidoId(), metadata.topic(), metadata.partition(), metadata.offset());
        return ResponseEntity.accepted().body(body);
    }
}
