# Kafka: preservar configuração e modo de operação

## Inspeção obrigatória

Antes de qualquer implementação Kafka, examine o build, `src`, propriedades de todos os perfis relevantes e infraestrutura local do projeto alvo. Procure `KafkaConfig`, `Producer`, `KafkaProducer`, `ProducerRecord`, `KafkaConsumer`, `@KafkaListener`, `KafkaTemplate`, serializers, deserializers e configurações de offsets. Identifique:

- operação: publicar, consumir ou ambas;
- transporte e autenticação: PLAINTEXT, SSL ou o modo já configurado;
- formato de chave e valor: texto, JSON ou Avro e Schema Registry;
- tópicos, chaves, grupos, confirmação, retries, timeouts e tratamento de falhas;
- API utilizada, beans, qualifiers, propriedades e perfis.

Reutilize os componentes existentes e siga sempre o padrão identificado. Não substitua clientes nativos por Spring Kafka, nem o inverso, por conveniência. Não altere envio bloqueante para assíncrono, commit, ordenação ou formato de mensagem silenciosamente. Requisitos explícitos do usuário prevalecem; explique mudanças necessárias ao contrato.

Se os dois modos abaixo estiverem disponíveis, selecione pelo contrato do tópico e pela configuração do ambiente. Não deduza o modo apenas do nome do tópico. Pergunte somente quando a inspeção e o pedido não resolverem uma escolha necessária.

## Base extraída de src

No projeto que originou esta skill, as fontes são `src/main/java/br/com/tiagoiwamoto/skill/config/KafkaConfig.java`, `src/main/java/br/com/tiagoiwamoto/skill/adapter/KafkaAdapterOut.java`, `src/main/resources/application.properties` e `pom.xml`. Estes caminhos documentam a origem; a skill distribuída não depende de acesso a esse repositório. Quando disponíveis, releia as fontes para detectar alterações.

O projeto usa clientes Apache Kafka nativos, beans Spring em `config` e publicação em `adapter`. O build observado declara `org.apache.kafka:kafka-clients:3.9.0`, `io.confluent:kafka-avro-serializer:7.8.0` e o repositório Maven `https://packages.confluent.io/maven/`. São versões da referência, não indicação de versões atuais: preserve o build existente e valide compatibilidade ao criar outro projeto. Adicione dependências Avro somente quando necessárias.

As propriedades Kafka estão nos defaults de `@Value` em `KafkaConfig`, não no `application.properties` atual. Preserve seus nomes ao externalizá-las.

### Publicação de texto

- Bean e qualifier `kafkaPlainTextProducer`, tipo `Producer<String, String>` e implementação `KafkaProducer`.
- `StringSerializer` para chave e valor. Uma mensagem String é enviada como recebida, sem serialização JSON adicional.
- `kafka.bootstrap.servers`: default local `localhost:9092`.
- `kafka.plain.acks`: `all`; `kafka.plain.retries`: `3`; `kafka.plain.delivery.timeout.ms`: `120000`.
- A referência não define `security.protocol` nesse producer; o modo é PLAINTEXT.

### Publicação SSL com Avro e Schema Registry

- Bean e qualifier `kafkaSslSchemaRegistryProducer`, tipo `Producer<String, Object>` e implementação `KafkaProducer`.
- Chave com `StringSerializer`, valor com `KafkaAvroSerializer` e `security.protocol=SSL`.
- `kafka.ssl.bootstrap.servers`: default local `localhost:9093`.
- `kafka.ssl.schema.registry.url`: default local `http://localhost:8081`, mapeado para `schema.registry.url`.
- `kafka.ssl.acks`: `all`; `kafka.ssl.retries`: `3`.
- `kafka.ssl.truststore.location`: quando ausente ou em branco, a referência resolve `${java.home}/lib/security/cacerts` em Java.
- `kafka.ssl.truststore.password`: obter do ambiente/segredo. A referência tem um default local de senha; não o replique como credencial de produção.
- `kafka.ssl.endpoint.identification.algorithm`: `https`; preserve a validação de hostname.
- `auto.register.schemas=false` e `use.latest.version=true`: mantenha o contrato de schemas previamente registrados. Não habilite registro automático para contornar erro de schema.
- `Object` na assinatura não torna qualquer POJO serializável em Avro. Use payload compatível com o schema e o serializer, como o registro Avro já adotado pelo projeto; não substitua Avro por JSON.

SSL do broker e segurança do Schema Registry são configurações distintas. Preserve as configurações de cada serviço. Endereços localhost são defaults para execução no host; confira os listeners anunciados quando a aplicação executar em container. Não copie certificados, senhas ou scripts de limpeza do ambiente de exemplo.

## Contrato do adapter de saída

Preserve os métodos da referência quando reutilizar este adapter:

```java
RecordMetadata publishPlainText(String topic, String message)
RecordMetadata publishPlainText(String topic, String key, String message)
RecordMetadata publishWithSslAndSchemaRegistry(String topic, Object payload)
RecordMetadata publishWithSslAndSchemaRegistry(String topic, String key, Object payload)
```

Os overloads sem chave delegam com `key=null`. Valide tópico nulo/em branco e payload nulo antes do envio. Crie `ProducerRecord` com tópico, chave e valor e use `producer.send(record).get()`, retornando `RecordMetadata` após confirmação.

Em `InterruptedException`, restaure a interrupção com `Thread.currentThread().interrupt()` antes de propagar uma falha com contexto. Em `ExecutionException`, preserve a causa original. Registre tópico, partição e offset; registre a chave apenas quando não contiver dados sensíveis. Não registre o corpo da mensagem.

Injete producers por construtor, com qualifiers explícitos nos parâmetros quando houver ambiguidade; não dependa da propagação de anotações de campo pelo Lombok sem verificar a configuração. Reutilize os beans, sem criar producer por mensagem, e preserve o fechamento no ciclo de vida da aplicação.

## Consumo: limite da referência

**O src de origem não contém consumidor Kafka nem configuração de grupo, polling, commit ou retry de consumo.** `ClientSqsConsumer` é SQS e não comprova um padrão Kafka. Não apresente `@KafkaListener`, confirmação manual, DLT ou qualquer configuração de consumer como implementação já existente nessa base.

Se o projeto alvo tiver consumidor Kafka, siga sua biblioteca, desserialização, tratamento de erros, concorrência e política de confirmação. Coloque adaptação de entrada em `entrypoint/kafka` quando compatível com a estrutura existente e delegue regras a `core/usecase`.

Se for o primeiro consumidor, derive transporte e formato do contrato do tópico e mantenha clientes nativos como ponto de partida desta referência. Defina explicitamente grupo, deserializers, política de offset/commit, tratamento de falhas e encerramento; documente-os como novas decisões. Não copie propriedades de producer (`acks`, serializers ou flags de registro de schema) para um consumer. Para texto, use desserialização String; para Avro, determine se o contrato utiliza registros específicos ou genéricos. Não invente garantias de entrega ou exactly-once.

## Validação da implementação gerada

Teste somente o modo implementado, cobrindo tópicos/chaves/payloads enviados, seleção do producer, retorno de metadata, rejeição de entradas inválidas, falha de envio e restauração de interrupção. Para um consumer, cubra processamento com sucesso e falha conforme a política de offsets escolhida. Testes unitários não precisam de broker real; valide SSL/Avro com integração quando fizer parte do escopo e houver infraestrutura disponível. Execute o Maven Wrapper do projeto e informe limitações de validação.
