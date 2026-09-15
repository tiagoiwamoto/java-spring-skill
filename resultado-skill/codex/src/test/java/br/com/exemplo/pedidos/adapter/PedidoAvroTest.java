package br.com.exemplo.pedidos.adapter;
import java.io.ByteArrayOutputStream;
import org.apache.avro.generic.*;
import org.apache.avro.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PedidoAvroTest {
    @Test void roundTripAvroBinary() throws Exception {
        var record = PedidoAvro.criar("p-1", "CRIADO");
        var bytes = new ByteArrayOutputStream();
        var encoder = EncoderFactory.get().binaryEncoder(bytes, null);
        new GenericDatumWriter<GenericRecord>(record.getSchema()).write(record, encoder);
        encoder.flush();
        var result = new GenericDatumReader<GenericRecord>(record.getSchema()).read(null,
                DecoderFactory.get().binaryDecoder(bytes.toByteArray(), null));
        assertEquals("p-1", result.get("pedidoId").toString());
        assertEquals("CRIADO", result.get("status").toString());
    }
}
