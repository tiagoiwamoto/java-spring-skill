# DECISOES — pedidos-kafka

Projeto Maven Java 25 / Spring Boot 4.1.0, pacote `br.com.exemplo.pedidos`, criado nesta
pasta seguindo a skill `java-spring` (`skill/java-spring.skill/`).

## Arquivos da skill consultados

- `skill/java-spring.skill/SKILL.md` — fluxo, invariantes (Java 25, Boot 4, Jackson 3
  `tools.jackson`, injeção por construtor, BOMs, falhas explícitas).
- `skill/java-spring.skill/references/architecture.md` — camadas `config`, `adapter`,
  `core/usecase`, `core/error`, `entrypoint/rest` e padrão de `ApiError` +
  `@RestControllerAdvice`.
- `skill/java-spring.skill/references/dependencies.md` — parent `spring-boot-starter-parent`,
  somente starters exigidos pelo escopo, test starters modulares do Boot 4.
- `skill/java-spring.skill/references/http.md` — `@RestController`, `@Valid`,
  normalização de erros.
- `skill/java-spring.skill/references/kafka.md` — os dois modos de publicação, contrato
  do adapter, validações, tratamento de `InterruptedException`/`ExecutionException` e
  requisitos de teste.
- Fontes de origem da skill (reli antes de substituí-las, conforme "releia as fontes"):
  `src/main/java/br/com/tiagoiwamoto/skill/config/KafkaConfig.java`,
  `src/main/java/br/com/tiagoiwamoto/skill/adapter/KafkaAdapterOut.java`,
  `src/main/resources/application.properties` e o `pom.xml` anterior.
- `references/aws-messaging.md` lido e descartado: o pedido proíbe AWS, SQS, SNS e
  consumidor.

Os fontes antigos `br/com/tiagoiwamoto/**` foram removidos de `src/` porque o pedido exige
o projeto diretamente nesta pasta e eles dependem de bibliotecas (AWS, JPA) fora do escopo
do novo `pom.xml`.

## Padrões aplicados

- **Arquitetura**: `PedidoEventoEntrypoint` (REST) → `PedidoEventoUsecase` →
  `KafkaAdapterOut` → `KafkaProducer` nativo. Sem regra de negócio no controller.
- **REST**: `POST /pedidos/eventos` recebe `PedidoEventoRequest` (record com Bean
  Validation), serializa o evento em JSON texto com o `tools.jackson.databind.ObjectMapper`
  configurado pelo Boot e publica como texto no tópico `pedidos.kafka.topic.eventos`,
  usando `pedidoId` como chave. Resposta `202 Accepted` com `PedidoEventoResponse`
  (pedidoId, topic, partition, offset). Erros normalizados por `RestExceptionHandler` +
  `ApiError` (instante, status, mensagem, path, violações): 400 para validação e argumentos
  inválidos, 502 para `KafkaPublishException`, 500 para `PedidoEventoSerializationException`.
- **Kafka (dois modos da referência)**: clientes `org.apache.kafka` nativos, beans em
  `KafkaConfig` com qualifiers `kafkaPlainTextProducer` (`Producer<String,String>`,
  `StringSerializer`, PLAINTEXT, acks/retries/delivery.timeout externalizados) e
  `kafkaSslSchemaRegistryProducer` (`Producer<String,Object>`, chave String, valor
  `KafkaAvroSerializer`, `security.protocol=SSL`, `schema.registry.url`,
  `auto.register.schemas=false`, `use.latest.version=true`,
  `ssl.endpoint.identification.algorithm=https`, truststore com fallback para
  `${java.home}/lib/security/cacerts`).
- **Contrato do adapter preservado**: os quatro métodos documentados
  (`publishPlainText` e `publishWithSslAndSchemaRegistry`, com e sem chave; overloads
  delegam com `key=null`), validação de tópico nulo/em branco e payload nulo,
  `producer.send(record).get()` bloqueante retornando `RecordMetadata`, log de
  tópico/partição/offset/chave sem registrar o corpo.
- **Falhas explícitas**: `KafkaPublishException` (domínio de integração) substitui o
  `RuntimeException` genérico da referência; em `InterruptedException` a flag é restaurada
  com `Thread.currentThread().interrupt()` antes de propagar; em `ExecutionException` a
  causa original é preservada.
- **Injeção**: construtor explícito com `@Qualifier` nos parâmetros (não depende de
  propagação de anotação de campo do Lombok, que exigiria `lombok.config` ausente).
- **Registro Avro compatível**: `src/main/avro/pedido-evento.avsc` gera
  `br.com.exemplo.pedidos.avro.PedidoEvento` (`SpecificRecord`) via `avro-maven-plugin`
  com `stringType=String`. É o payload esperado pelo modo SSL/Avro; o schema deve estar
  previamente registrado no Schema Registry.
- **Dependências**: `kafka-clients` sem versão fixa — gerenciado pelo BOM do Boot 4.1.0
  (resolve `kafka.version` 4.2.1, versão mais recente que a referência 3.9.0, conforme
  "versões da referência não indicam versões atuais"). `kafka-avro-serializer` 7.8.0
  (mesma versão da referência) e `avro` 1.11.4 (versão transitiva do serializer, alinhada
  com o plugin de codegen). Repositório `https://packages.confluent.io/maven/` mantido.
  Testes com `spring-boot-starter-webmvc-test` (JUnit 6, Mockito, AssertJ, MockMvc).
- **Maven Wrapper**: `mvnw`, `mvnw.cmd` e `.mvn/wrapper/maven-wrapper.properties`
  (wrapper 3.3.4, `only-script`, Maven 3.9.16) já presentes na pasta e mantidos.

## Propriedades exigidas (application.properties, com defaults locais seguros)

| Propriedade | Default | Uso |
|---|---|---|
| `pedidos.kafka.topic.eventos` | `pedidos-eventos` | Tópico do POST /pedidos/eventos |
| `kafka.bootstrap.servers` | `localhost:9092` | Producer texto PLAINTEXT |
| `kafka.plain.acks` | `all` | Confirmação do producer texto |
| `kafka.plain.retries` | `3` | Retries do producer texto |
| `kafka.plain.delivery.timeout.ms` | `120000` | Timeout de entrega |
| `kafka.ssl.bootstrap.servers` | `localhost:9093` | Producer SSL/Avro |
| `kafka.ssl.schema.registry.url` | `http://localhost:8081` | Schema Registry |
| `kafka.ssl.acks` | `all` | Confirmação do producer SSL |
| `kafka.ssl.retries` | `3` | Retries do producer SSL |
| `kafka.ssl.endpoint.identification.algorithm` | `https` | Validação de hostname |
| `kafka.ssl.truststore.location` | vazio → `${java.home}/lib/security/cacerts` | Truststore SSL |
| `kafka.ssl.truststore.password` | `changeit` via env `KAFKA_SSL_TRUSTSTORE_PASSWORD` | Senha do truststore |

Todas aceitam variáveis de ambiente (ex.: `KAFKA_BOOTSTRAP_SERVERS`). O default `changeit`
é apenas a senha pública do `cacerts` do JDK para desenvolvimento local — não é credencial
de produção; em ambientes reais, fornecer `KAFKA_SSL_TRUSTSTORE_PASSWORD` do segredo do
ambiente.

## Validação executada

- `sh ./mvnw -B -ntp clean test` — BUILD SUCCESS, 13 testes, 0 falhas:
  - `KafkaAdapterOutTest` (10): sucesso texto/Avro com e sem chave, captura de
    `ProducerRecord` (tópico, chave, valor, instância Avro), tópico nulo/em branco,
    payload nulo, falha do producer preservando a causa, restauração de interrupção.
  - `PedidoEventoEntrypointTest` (3, `@WebMvcTest` sem broker): 202 com metadata,
    400 com violações, 502 em falha de publicação.

## Limitações da validação

- Nenhum broker Kafka nem Schema Registry foi usado; producers foram mockados. O modo
  SSL/Avro não foi exercitado de ponta a ponta (handshake TLS, serialização Avro real,
  lookup de schema).
- Em runtime, o modo Avro exige schema previamente registrado (`auto.register.schemas=false`);
  um `PedidoEvento` não registrado falhará na serialização.
- A compatibilidade kafka-clients 4.2.1 × kafka-avro-serializer 7.8.0 só foi validada por
  compilação/testes unitários, não por tráfego real.
- Compatibilidade do schema `.avsc` com um contrato de tópico existente não foi verificada
  (não há registry disponível).
