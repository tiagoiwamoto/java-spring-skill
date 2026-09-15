package br.com.exemplo.pedidos.adapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
public final class PedidoAvro {
    private static final Schema SCHEMA = load();
    private PedidoAvro() {}
    public static GenericRecord criar(String pedidoId, String status) {
        var record = new GenericData.Record(SCHEMA);
        record.put("pedidoId", Objects.requireNonNull(pedidoId));
        record.put("status", Objects.requireNonNull(status));
        return record;
    }
    private static Schema load() {
        try (InputStream in = PedidoAvro.class.getResourceAsStream("/avro/pedido-evento.avsc")) {
            return new Schema.Parser().parse(Objects.requireNonNull(in, "Schema ausente"));
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
}
