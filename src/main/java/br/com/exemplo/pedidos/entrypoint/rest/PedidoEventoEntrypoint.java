package br.com.exemplo.pedidos.entrypoint.rest;

import br.com.exemplo.pedidos.core.usecase.PedidoEventoUsecase;
import br.com.exemplo.pedidos.entrypoint.rest.dto.PedidoEventoRequest;
import br.com.exemplo.pedidos.entrypoint.rest.dto.PedidoEventoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoEventoEntrypoint {

    private final PedidoEventoUsecase usecase;

    @PostMapping("/eventos")
    public ResponseEntity<PedidoEventoResponse> publicarEvento(
            @Valid @RequestBody PedidoEventoRequest request
    ) {
        var metadata = usecase.publicarEventoTexto(request);
        var response = new PedidoEventoResponse(
                request.pedidoId(),
                metadata.topic(),
                metadata.partition(),
                metadata.offset()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
