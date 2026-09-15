# Arquitetura e organização

Adote esta separação como ponto de partida e ajuste-a à complexidade real:

```text
<base-package>/
├── config/                 configuração técnica e criação de beans
├── entrypoint/
│   ├── rest/               controllers, advice e DTOs de transporte
│   └── sqs/                listeners e adaptação da mensagem recebida
├── core/
│   ├── entity/             modelo persistido ou de domínio
│   ├── error/              exceções específicas
│   ├── mapper/             MapStruct
│   ├── repository/         contratos de persistência
│   └── usecase/            orquestração e regras de aplicação
└── adapter/                persistência, APIs externas, SNS e SQS de saída
```

Não crie diretórios vazios nem camadas cerimoniais sem uso. Em domínios maiores, prefira organizar primeiro por feature e manter essas fronteiras dentro da feature.

## Responsabilidades

- Entrypoint valida e traduz entrada/saída; não contém regra de negócio.
- Use case coordena regras e dependências.
- Adapter encapsula tecnologia externa e traduz suas falhas.
- Repository expõe a persistência necessária ao caso de uso.
- Mapper converte entre entidade e DTO sem lógica de negócio.
- Config cria clientes e políticas técnicas reutilizáveis.

Use `@RequiredArgsConstructor` para componentes Spring com dependências finais. Use `@Slf4j` somente onde existam eventos relevantes para registrar. Não registre payloads sensíveis.

## DTOs, respostas e erros

- Prefira records para request/response e eventos.
- Aplique Bean Validation na fronteira de entrada.
- Quando o projeto usar envelope de resposta, mantenha um tipo genérico como `AppData<T>` de forma consistente.
- Use `@RestControllerAdvice` para produzir um erro estruturado contendo instante, status, mensagem, caminho e violações.
- Preserve exceções conhecidas antes de capturar falhas técnicas. Uma exceção `NotFound`, por exemplo, não deve ser engolida por um `catch (Exception)` e convertida em erro de banco.

## Persistência e mapeamento

- Use Spring Data JPA somente quando houver persistência relacional.
- Use MapStruct para conversões repetidas entre DTOs e entidades.
- Configure `mapstruct-processor`, Lombok e `lombok-mapstruct-binding` como annotation processors quando ambos forem usados.
- Testes unitários cobrem casos de uso e adapters; testes de slice ou integração cobrem JPA e endpoints quando agregarem confiança real.
