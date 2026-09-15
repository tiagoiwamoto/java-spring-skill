# SNS e SQS com AWS SDK v2

## Clientes

- Crie `SnsClient` e `SqsClient` como beans.
- Externalize região, ambiente, endpoints e retry.
- Em ambiente local, permita endpoint do LocalStack e credenciais locais/anonymous apenas nesse perfil.
- Fora do ambiente local, use `DefaultCredentialsProvider`; nunca grave chaves.
- Use `ClientOverrideConfiguration.retryStrategy`, não a API descontinuada `RetryPolicy`.
- Use a estratégia standard, `maxAttempts` como total incluindo a primeira chamada, circuit breaker do SDK e `BackoffStrategy.exponentialDelay` com full jitter.
- Configure backoff normal e throttling separadamente. Defaults razoáveis são 100 ms, 1 s para throttling e teto de 20 s.

Exponha propriedades com unidades de duração, por exemplo:

```yaml
app:
  aws:
    region: sa-east-1
    environment: local
    sqs:
      retry:
        attempts: 3
        backoff:
          base-delay: 100ms
          throttling-base-delay: 1s
          max-delay: 20s
```

## Jackson

Use o `tools.jackson.databind.ObjectMapper` configurado pelo Boot. Um conversor específico pode encapsular `readValue` e `writeValueAsString`, lançando uma exceção de integração com contexto de SNS ou SQS. Não declare um mapper concorrente em cada configuração.

## Adapters de publicação

Crie adapters de saída separados para SNS e SQS. Todos os overloads de `publish` devem exigir `payload` e `Class<T> payloadType`, validar `payloadType.isInstance(payload)` e retornar a resposta do SDK.

Ofereça estas formas, adaptadas para `PublishResponse` ou `SendMessageResponse`:

```java
publish(payload, payloadType)
publish(payload, payloadType, headers)
publish(payload, payloadType, messageGroupId)
publish(payload, payloadType, messageGroupId, deduplicationId, headers)
publish(payload, payloadType, PublishOptions options)
```

- Serialize `String` sem aspas JSON e demais objetos pelo conversor Jackson.
- Converta `Map<String, String>` em message attributes com `dataType("String")`.
- Para SNS use `topicArn`; para SQS use `queueUrl`; ambos devem vir de configuração externalizada.
- Valide ARN/URL, payload e tipo antes de chamar o SDK.
- Registre identificador retornado, destino e indicador FIFO, sem registrar o corpo.

## FIFO

Modele opções imutáveis contendo `fifo`, `messageGroupId`, `messageDeduplicationId` e headers.

- Exija `messageGroupId` quando `fifo` for verdadeiro.
- Inclua `messageGroupId` e `messageDeduplicationId` no request FIFO.
- Gere UUID quando o deduplication ID não for informado, mas documente que retries idempotentes exigem um ID determinístico do evento.
- Não envie campos FIFO para destinos standard.

## Consumidores e testes

Listeners SQS recebem DTO tipado e headers apenas quando necessários. Configure acknowledgement de acordo com a semântica desejada; `ON_SUCCESS` evita confirmar processamento que lançou erro.

Nos testes dos adapters, capture o request enviado ao cliente mockado e verifique:

- serialização e destino;
- message attributes;
- ausência de campos FIFO no modo standard;
- group ID e deduplication ID no modo FIFO;
- geração de deduplication ID quando omitido;
- rejeição de group ID vazio e incompatibilidade entre payload e tipo declarado.
