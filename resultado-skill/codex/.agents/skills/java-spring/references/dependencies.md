# Plataforma e dependências

Para projetos novos, use uma versão estável e mutuamente compatível de:

- Java 25;
- Spring Boot 4.x;
- Spring Framework 7.x, trazido pelo Boot;
- Jackson 3, com imports `tools.jackson`;
- Spring Cloud AWS 4.x quando houver AWS.

Confira versões atuais em fontes oficiais quando a data da geração puder torná-las obsoletas. Não use milestone, RC ou snapshot sem solicitação explícita.

## Maven

Use `spring-boot-starter-parent` e propriedades para versões não gerenciadas. Adicione somente starters necessários ao escopo, por exemplo:

- `spring-boot-starter-web` e `spring-boot-starter-validation` para API MVC;
- `spring-boot-starter-restclient` para clientes HTTP declarativos;
- `spring-boot-starter-data-jpa` para persistência relacional;
- `spring-boot-starter-actuator` e um registry Micrometer compatível para observabilidade;
- `spring-cloud-aws-starter-sns` e/ou `spring-cloud-aws-starter-sqs` para mensageria AWS.

Para Spring Cloud AWS, importe o BOM e omita versões dos módulos e do AWS SDK administrados por ele:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-dependencies</artifactId>
            <version>${spring-cloud-aws.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Inclua Maven Wrapper. Separe dependências de teste e use os starters de teste modulares do Boot 4 quando adequados. Execute no mínimo:

```text
./mvnw test
```

Não adicione H2, devtools, Lambda, SNS, SQS, JPA ou OTLP apenas porque aparecem no projeto de referência; cada dependência deve servir a um requisito.

Para Kafka, leia [kafka.md](kafka.md). A base usa `kafka-clients` diretamente e o serializer Confluent para Avro; preserve a biblioteca do projeto alvo e inclua o serializer e seu repositório somente quando o modo exigir.
