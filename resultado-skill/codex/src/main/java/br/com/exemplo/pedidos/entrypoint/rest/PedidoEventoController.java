package br.com.exemplo.pedidos.entrypoint.rest;
import br.com.exemplo.pedidos.core.usecase.PublicarEventoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/pedidos/eventos")
@RequiredArgsConstructor
public class PedidoEventoController {
    private final PublicarEventoPedido publicar;
    public record EventoRequest(String chave, @NotNull String mensagem) {}
    public record EventoResponse(String topico, int particao, long offset) {}
    @PostMapping
    public EventoResponse publicar(@Valid @RequestBody EventoRequest request) {
        var metadata = publicar.executar(request.chave(), request.mensagem());
        return new EventoResponse(metadata.topic(), metadata.partition(), metadata.offset());
    }
}
