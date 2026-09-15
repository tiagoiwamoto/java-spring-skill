# DECISOES — pedidos-kafka

Projeto Maven Java 25 / Spring Boot 4.1.1, pacote `br.com.exemplo.pedidos`, criado nesta
pasta seguindo a skill `java-spring` (instalada em `.agents/skills/java-spring/`, origem
`skill/java-spring.skill/` do repositório).

## Arquivos da skill consultados

- `.agents/skills/java-spring/SKILL.md` — fluxo e invariantes: Java 25, Spring Boot 4,
  Jackson 3 (`tools.jackson`), injeção por construtor com campos `final`, records para
  DTOs, BOMs, falhas explícitas de domínio/integração, um único `ObjectMapper`.
- `references/architecture.md` — camadas `config`, `adapter`, `core/entity`,
  `core/error`, `core/usecase`, `entrypoint/rest` + `dto`; regras fora do controller;
  `ApiError` + `@RestControllerAdvice` com instante, status, mensagem, caminho e violações.
- `references/dependencies.md` — `spring-boot-starter-parent`, somente starters do
  escopo, test starters modulares do Boot 4, repositório Confluent somente porque o modo
  Avro o exige.
- `references/http.md` — `@RestController`, `@Valid` no DTO, normalização de erros.
- `references/kafka.md` — inspeção obrigatória (esta pasta não tinha código-fonte
  próprio; a base documentada é a referência da skill), os dois modos de publicação, o
  contrato do adapter, validações, tratamento de `InterruptedException`/`ExecutionException`
  e requisitos de teste sem broker.
- `references/aws-messaging.md` lido e descartado: o pedido proíbe AWS, SQS e SNS.

## Padrões aplicados

- **Arquitetura**: `PedidoEventoController` (REST) → `PedidoEventoUsecase` →
  `KafkaAdapterOut` → `KafkaProducer` nativo (`org.apache.kafka`, sem Spring Kafka).
  Não há consumidor, banco, AWS nem infraestrutura remota.
- **REST**: `POST /pedidos/eventos` recebe `PedidoEventoRequest` (record com Bean
  Validation `pedidoId`/`tipoEvento` obrigatórios, `detalhe` opcional), o use case monta
  o evento de domínio `EventoPedido` (com `ocorridoEm`) e o serializa em JSON texto com o
  `tools.jackson.databind.ObjectMapper` configurado pelo Boot. Publicação como texto no
  tópico `pedidos.kafka.topic.eventos` com `pedidoId` como chave. Resposta `202 Accepted`
  com `PedidoEventoResponse` (pedidoId, topic, partition, offset). Erros normalizados por
  `RestExceptionHandler` + `ApiError`: 400 para Bean Validation e `IllegalArgumentException`,
  502 para `KafkaPublishException`, 500 para `PedidoEventoSerializationException`.
- **Kafka — modo texto**: bean `kafkaPlainTextProducer` (`Producer<String,String>`,
  `StringSerializer` chave/valor, sem `security.protocol` → PLAINTEXT), com acks, retries e
  delivery.timeout.ms externalizados.
- **Kafka — modo SSL/Avro**: bean `kafkaSslSchemaRegistryProducer`
  (`Producer<String,Object>`, chave `StringSerializer`, valor `KafkaAvroSerializer`,
  `security.protocol=SSL`, `schema.registry.url`, `auto.register.schemas=false`,
  `use.latest.version=true`, `ssl.endpoint.identification.algorithm=https`, truststore com
  fallback para `${java.home}/lib/security/cacerts` quando a propriedade está em branco).
- **Contrato do adapter** (conforme `references/kafka.md`): os quatro métodos
  `publishPlainText`/`publishWithSslAndSchemaRegistry` com e sem chave (overloads delegam
  com `key=null`), validação de tópico nulo/em branco e payload nulo antes do envio,
  `producer.send(record).get()` bloqueante retornando `RecordMetadata`, log de
  tópico/partição/offset/chave sem registrar o corpo da mensagem.
- **Falhas explícitas**: `KafkaPublishException` para falhas de publicação; em
  `InterruptedException` a flag é restaurada com `Thread.currentThread().interrupt()`
  antes de propagar; em `ExecutionException` a causa original é preservada.
- **Injeção**: construtores explícitos com `@Qualifier` nos parâmetros do adapter (o
  projeto não usa Lombok, então não há dependência de propagação de anotações de campo).
- **Registro Avro compatível**: `src/main/avro/pedido-evento.avsc` gera
  `br.com.exemplo.pedidos.avro.PedidoEvento` (`SpecificRecord`) via `avro-maven-plugin`
  com `stringType=String`. O use case `publicarComoAvro` constrói o registro pelo builder
  gerado. O modo Avro exige o schema previamente registrado no Schema Registry
  (`auto.register.schemas=false`).
- **Dependências**: `kafka-clients` sem versão fixa — gerenciado pelo BOM do Boot 4.1.1
  (resolveu `kafka-clients` 4.2.1). `kafka-avro-serializer` 8.3.1 (GA atual no repositório
  Confluent; a referência citava 7.8.0 apenas como versão observada, não como indicação de
  versão atual) e `avro`/`avro-maven-plugin` 1.12.2. Repositório
  `https://packages.confluent.io/maven/` declarado porque o serializer Confluent não está
  no Maven Central. Testes com `spring-boot-starter-webmvc-test` (JUnit, Mockito, AssertJ,
  MockMvc, slice `@WebMvcTest`).
- **Maven Wrapper**: `mvnw`, `mvnw.cmd` e `.mvn/wrapper/maven-wrapper.properties`
  (wrapper 3.3.4, `only-script`, Maven 3.9.16).

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
de produção; em ambientes reais, fornecer `KAFKA_SSL_TRUSTSTORE_PASSWORD` a partir do
segredo do ambiente.

## Validação executada

- `sh ./mvnw -B -ntp clean test` em 14/09/2026 com Java 25.0.4.1 — **BUILD SUCCESS**,
  16 testes, 0 falhas, 0 erros, 0 ignorados:
  - `KafkaAdapterOutTest` (9): sucesso texto com e sem chave, sucesso SSL/Avro com e sem
    chave (captura de `ProducerRecord`: tópico, chave, payload `PedidoEvento`, seleção do
    producer correto), tópico nulo, tópico em branco, payload nulo nos dois modos, falha
    do producer (`ExecutionException`) propagada como `KafkaPublishException` com a causa
    preservada, e `InterruptedException` com restauração da flag de interrupção.
  - `KafkaConfigTest` (2): fallback do truststore para o `cacerts` do JDK e preservação de
    localização configurada.
  - `PedidoEventoUsecaseTest` (2): publicação texto com JSON no tópico configurado e
    `pedidoId` como chave; publicação Avro com registro `PedidoEvento` construído corretamente.
  - `PedidoEventoControllerTest` (3, `@WebMvcTest` sem broker): 202 com metadata,
    400 com violações de Bean Validation, 502 em `KafkaPublishException`.
- `sh ./mvnw -B -ntp dependency:list` confirmou `kafka-clients` 4.2.1,
  `kafka-avro-serializer` 8.3.1 e `avro` 1.12.2 no classpath.

## Limitações da validação

- Nenhum broker Kafka nem Schema Registry foi usado; os producers foram mockados. O modo
  SSL/Avro não foi exercitado de ponta a ponta (handshake TLS, serialização Avro real,
  consulta de schema no registry).
- Em runtime, o modo Avro exige schema previamente registrado (`auto.register.schemas=false`);
  um `PedidoEvento` não registrado falhará na serialização.
- A compatibilidade kafka-clients 4.2.1 × kafka-avro-serializer 8.3.1 foi validada por
  resolução de dependências, compilação e testes unitários — não por tráfego real.
- A compatibilidade do schema `.avsc` com um contrato de tópico existente não foi
  verificada (não há Schema Registry disponível).
