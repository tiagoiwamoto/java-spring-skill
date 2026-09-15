---
name: java-spring
description: Cria e evolui projetos Java Spring seguindo os padrões do projeto de referência em src, incluindo REST, JPA, SNS, SQS e Kafka. Use ao criar ou estruturar projetos Java Spring ou implementar, configurar e revisar consumo ou publicação Kafka, preservando o modo e as convenções existentes.
---

# Java Spring Project

Crie projetos executáveis e coerentes com o domínio solicitado. Preserve nomes, requisitos, ferramenta de build e infraestrutura escolhidos pelo usuário; não acrescente integrações que ele não pediu.

## Fluxo

1. Descubra no contexto o nome do projeto, pacote-base, domínio, endpoints, persistência e integrações. Pergunte somente quando uma escolha ausente mudar materialmente o resultado.
2. Antes de gerar arquivos, leia [references/architecture.md](references/architecture.md) e [references/dependencies.md](references/dependencies.md).
3. Se houver API REST ou cliente HTTP, leia [references/http.md](references/http.md).
4. Se houver SNS, SQS ou execução local com LocalStack, leia [references/aws-messaging.md](references/aws-messaging.md).
5. Sempre que a tarefa envolver Kafka, leia [references/kafka.md](references/kafka.md) antes de implementar. Inspecione configurações, producers e consumers do projeto alvo para identificar o modo de consumir ou publicar e siga o mesmo padrão. Não troque biblioteca, serialização, transporte ou semântica de entrega sem requisito explícito.
6. Gere apenas os componentes usados pelo projeto. Não copie nomes de domínio do projeto de referência, como `Client`, `ViaCep` ou seus pacotes. Em projetos existentes, preserve a estrutura e as versões compatíveis já adotadas.
7. Centralize configurações externalizáveis e forneça defaults somente quando forem seguros para desenvolvimento local. Nunca grave credenciais.
8. Compile e execute os testes com o wrapper do projeto. Corrija falhas causadas pela geração antes de concluir.

## Invariantes

- Use Java 25, Spring Boot 4 e Jackson 3 (`tools.jackson`) para um projeto novo, salvo restrição explícita do usuário ou incompatibilidade confirmada.
- Use injeção por construtor e campos `final`; prefira records para DTOs imutáveis.
- Mantenha regras de negócio fora de controllers, listeners, repositórios e clientes externos.
- Use BOMs para famílias de dependências e não fixe versões individuais já gerenciadas.
- Modele falhas de domínio e integração de forma explícita; não converta indiscriminadamente exceções de negócio em falhas técnicas.
- Não registre múltiplos beans equivalentes de `ObjectMapper`; reutilize o Jackson 3 configurado pelo Spring Boot.
- Teste comportamento observável e os limites das integrações, não apenas a inicialização do contexto.

Ao concluir, informe a estrutura criada, decisões importantes, propriedades necessárias e comandos de validação executados.
