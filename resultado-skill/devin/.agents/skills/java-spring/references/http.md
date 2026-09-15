# APIs REST e clientes HTTP

## Entrada REST

- Use `@RestController`, rota base clara e `ResponseEntity` quando o status ou headers precisarem ser controlados.
- Use `@Valid` nos DTOs e `@Validated` quando houver validação de parâmetros.
- Normalize erros com `@RestControllerAdvice`.
- Não exponha diretamente entidades JPA.

## Saída HTTP

Para Spring Boot 4, prefira `RestClient` e HTTP interfaces quando o contrato externo for estável:

1. Declare uma interface com as operações remotas.
2. Crie `RestClient` com base URL e headers externalizados.
3. Gere o proxy com `RestClientAdapter` e `HttpServiceProxyFactory`.
4. Traduza erros HTTP e falhas de comunicação na fronteira do adapter.

Retries devem se limitar a falhas transitórias, normalmente I/O, HTTP 408, 429 e 5xx. Respeite o total máximo de tentativas, feche respostas antes de repetir e preserve a interrupção da thread. Não repita automaticamente operações não idempotentes sem uma estratégia explícita de idempotência. Prefira backoff exponencial com jitter a espera linear fixa quando houver concorrência significativa.
