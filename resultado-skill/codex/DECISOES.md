# pedidos-kafka

Projeto Maven diretamente nesta pasta, pacote `br.com.exemplo.pedidos`, Java 25 e Spring Boot 4.1.1. Kafka nativo gerenciado pelo BOM do Boot; serializer Confluent 8.2.0 e Avro 1.12.1 com propriedades de versão próprias. Compatibilidade de compilação e execução ainda não comprovada neste ambiente.

## Referências consultadas

- `.agents/skills/java-spring/SKILL.md`
- `.agents/skills/java-spring/references/architecture.md`
- `.agents/skills/java-spring/references/dependencies.md`
- `.agents/skills/java-spring/references/http.md`
- `.agents/skills/java-spring/references/kafka.md` (integralmente, antes da implementação)
- Instrução RTK indicada pelo usuário em `/home/tiagoiwamoto/.codex/RTK.md`.
- Fontes oficiais para versões: https://spring.io/projects/spring-boot e https://docs.confluent.io/platform/8.2/release-notes/index.html ; serializer: https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-avro.html .

A inspeção inicial encontrou apenas diretórios de metadados e `_teste-skill`; não havia build, fontes ou configuração Kafka do projeto alvo. Não foram lidos resultados de outras IAs nem fontes do repositório de origem. `.agents` e `_teste-skill` não foram alterados.

## Padrões e contrato

- `config` cria producers reutilizados e fechados pelo Spring; `entrypoint/rest` valida entrada e traduz erros; `core/usecase` coordena publicação; `adapter` encapsula Kafka e suas falhas.
- Dependências finais, injeção por construtor e `@RequiredArgsConstructor`; o adapter tem construtor explícito para garantir qualifiers nos parâmetros. Records para request, response e erro. Sem ObjectMapper adicional: MVC usa Jackson 3 do Boot.
- `POST /pedidos/eventos`, `Content-Type: application/json`, corpo `{"chave":"p-1","mensagem":"pedido criado"}`. `chave` é opcional. O conteúdo de `mensagem` é enviado exatamente como recebido, sem JSON adicional; texto vazio é válido em Kafka, null não é. O tópico vem da configuração, nunca do cliente REST.
- Retorno HTTP 200 somente após confirmação: `{"topico":"pedidos-eventos","particao":0,"offset":7}` (exemplo, não resultado observado). HTTP 400 para entrada inválida; 502 quando não é possível confirmar publicação. Erros contêm instante, status, mensagem, caminho e violações, sem payload ou causa técnica exposta.
- Os quatro overloads documentados são preservados. Sem chave delega com null. Ambos os modos usam `send(record).get()`, retornam metadata, preservam causa assíncrona e restauram interrupção. Logs somente de tópico, partição e offset.
- Texto: bean `kafkaPlainTextProducer`, `Producer<String,String>`, StringSerializer nos dois campos, PLAINTEXT.
- Avro: bean `kafkaSslSchemaRegistryProducer`, `Producer<String,Object>`, StringSerializer na chave e KafkaAvroSerializer no valor, SSL, `auto.register.schemas=false`, `use.latest.version=true`. O adapter restringe Object a registros Avro válidos, rejeitando POJOs e registros com campos incompatíveis.
- `PedidoAvro.criar("p-1", "CRIADO")` produz GenericRecord a partir de `src/main/resources/avro/pedido-evento.avsc`; pode ser passado a `publishWithSslAndSchemaRegistry(topico, chave, registro)`. O endpoint solicitado usa somente texto.
- O schema deve estar previamente registrado no Schema Registry, compatível com a versão mais recente do subject `<topico>-value` (estratégia padrão do serializer). Nenhum schema é registrado por este projeto. Compatibilidade remota não é comprovada pela validação local do registro.
- Sem consumidor, banco, AWS, infraestrutura, retries de aplicação, commit, push ou deploy. Não há promessa de exactly-once: falha ou interrupção pode ocorrer depois da entrega; repetição pelo cliente pode duplicar eventos.

## Configurações

`src/main/resources/application.properties` centraliza os defaults locais e aliases de ambiente. Ambos os beans são criados no startup; configure o SSL também quando usar apenas o endpoint REST. Não há credenciais embutidas.

| Propriedade | Default/requisito |
| --- | --- |
| `server.port` | 8080 / `SERVER_PORT` |
| `pedidos.kafka.topic` | pedidos-eventos / `PEDIDOS_KAFKA_TOPIC` |
| `kafka.bootstrap.servers` | localhost:9092 / `KAFKA_BOOTSTRAP_SERVERS` |
| `kafka.plain.acks`, `.retries`, `.delivery.timeout.ms` | all, 3, 120000 |
| `kafka.plain.request.timeout.ms`, `.max.block.ms` | 30000, 60000 |
| `kafka.ssl.bootstrap.servers` | localhost:9093 |
| `kafka.ssl.schema.registry.url` | http://localhost:8081; configure HTTPS conforme o serviço |
| `kafka.ssl.acks`, `.retries`, `.delivery.timeout.ms` | all, 3, 120000 |
| `kafka.ssl.request.timeout.ms`, `.max.block.ms` | 30000, 60000 |
| `kafka.ssl.truststore.location` | ausente/branco resolve java.home/lib/security/cacerts |
| `kafka.ssl.truststore.password` | opcional, fornecida por ambiente/segredo conforme truststore; sem default de senha |
| `kafka.ssl.endpoint.identification.algorithm` | https; preserve validação de hostname |

Os aliases explícitos usam o nome em maiúsculas com pontos substituídos por underscores, por exemplo `KAFKA_SSL_DELIVERY_TIMEOUT_MS`. Propriedades opcionais adicionais podem ser fornecidas via arquivo externo Spring (`--spring.config.additional-location=...`) ou argumentos `--nome=valor`; prefira arquivo/secret para credenciais.

Broker: `kafka.ssl.truststore.type`, `kafka.ssl.keystore.location`, `.keystore.password`, `.keystore.type`, `.key.password` para truststore específico e mTLS. Schema Registry, independentemente do broker: `kafka.ssl.schema.registry.ssl.truststore.location`, `.ssl.truststore.password`, `.ssl.truststore.type`, `.ssl.keystore.location`, `.ssl.keystore.password`, `.ssl.key.password`, `.basic.auth.credentials.source` e `.basic.auth.user.info`. Valores não vazios são mapeados para as propriedades correspondentes com prefixo `schema.registry.`. Defaults localhost são para execução no host; listeners anunciados devem ser acessíveis.

## Wrapper e validação

Incluídos `mvnw` executável, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` e bootstrap PowerShell. É um bootstrap local no formato only-script, não uma cópia dos scripts oficiais: o download oficial foi tentado e falhou por DNS. Não necessita JAR de wrapper. Fixa Maven 3.9.11, baixa ZIP e verifica SHA-512 publicado no mesmo repositório antes de extrair. POSIX exige curl, unzip, sha512sum e Java; Windows exige PowerShell e Java. Distribuição e cache Maven ficam em `.mvn/distributions/`, dentro deste diretório. Windows não foi validado.

Comandos executados:

- `java -version`: Temurin 25.0.4.1, disponível.
- `mvn -version`: Maven não instalado.
- Tentativas de acesso Maven Central e GitHub: falha de resolução DNS.
- `./mvnw test`: executado, encerrou com código 6: `curl: (6) Could not resolve host: repo.maven.apache.org`. Não chegou à compilação nem à execução dos testes.

Testes escritos, ainda não executados: ambos os producers (sucesso, metadata, tópico/payload inválidos, chave presente/ausente, falha imediata/assíncrona e restauração de interrupção); round-trip binário Avro local; configuração dos dois modos e isolamento das credenciais; REST com validação, sucesso e erro de integração. Mockito usa mock maker subclass, sem necessidade de anexar agente JVM. Nenhum teste requer broker. SSL real, autenticação, Schema Registry, compatibilidade remota e entrega Kafka exigem infraestrutura e não foram validados.

Para validar com acesso aos repositórios, execute `./mvnw test`; para iniciar, `./mvnw spring-boot:run` com configurações adequadas. O resultado dos testes permanece desconhecido; não há alegação de build aprovado.

Verificações estáticas adicionais: `sh -n mvnw` passou; parsing XML do POM e JSON do schema passou; presença dos arquivos de bootstrap e permissão executável de `mvnw` verificadas. Essas verificações não substituem compilação/testes Java.
