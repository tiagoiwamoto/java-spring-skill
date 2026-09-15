# Como criei uma skill para desenvolver com Java e Spring Boot no meu estilo

Usar IA para desenvolver software fica mais interessante quando ela consegue seguir as decisões que já fazem parte do nosso trabalho: organização de pacotes, separação de responsabilidades, tratamento de erros e testes.

Foi com esse objetivo que criei a skill **java-spring**. Queria transformar minhas convenções de desenvolvimento em instruções reutilizáveis, para não precisar explicar a mesma arquitetura a cada tarefa.

Para colocar a ideia à prova, usei um exemplo com **Java, Spring Boot e Kafka**: uma API que recebe eventos de pedidos e publica mensagens.

Neste artigo, compartilho como organizei a skill, os resultados registrados no experimento e como essa abordagem pode acelerar o desenvolvimento, reduzir instruções repetidas e ajudar a IA a produzir código no meu estilo.

## Por que transformar minhas instruções em uma skill?

Quem desenvolve com IA provavelmente já passou por uma situação parecida: a implementação funciona, mas a organização do código exige ajustes.

O controller concentra regras de negócio. A integração fica misturada com o caso de uso. Os DTOs seguem outra convenção. O tratamento de erros varia entre endpoints.

Cada uma dessas diferenças gera uma nova rodada de orientação e revisão.

A proposta da minha skill foi reunir essas decisões em um lugar que o agente pudesse consultar. Em vez de reconstruir um prompt enorme para cada funcionalidade, passei a manter um conjunto versionado de instruções sobre como desenvolver.

A skill funciona como um guia de trabalho que acompanha a tarefa. Ela descreve padrões, indica referências e define o que precisa ser validado antes de concluir a implementação.

## Como organizei a java-spring

A estrutura é simples:

```text
java-spring.skill/
├── SKILL.md
└── references/
    ├── architecture.md
    ├── dependencies.md
    ├── http.md
    ├── kafka.md
    └── aws-messaging.md
```

O `SKILL.md` concentra o fluxo principal e as convenções comuns. Os arquivos de referência detalham assuntos específicos.

Essa divisão permite direcionar a consulta conforme a tarefa. Uma implementação Kafka precisa das orientações de Kafka; uma integração HTTP tem sua própria referência.

Entre as decisões documentadas estão:

- Organização em `entrypoint`, `core`, `adapter` e `config`.
- Regras de negócio fora dos controllers e das integrações.
- Injeção por construtor e campos `final`.
- Preferência por records para DTOs imutáveis.
- Configurações externalizadas.
- Tratamento explícito de falhas de domínio e integração.
- Testes de comportamento e execução pelo Maven Wrapper.

Para projetos novos, a skill estabelece Java 25, Spring Boot 4 e Jackson 3, respeitando restrições explícitas e compatibilidade. Em projetos existentes, orienta preservar a estrutura e as versões já adotadas.

Esse cuidado é parte do que considero “desenvolver no meu estilo”: produzir uma solução coerente com o contexto do projeto.

## O exemplo: uma aplicação de pedidos com Kafka

O cenário escolhido foi um serviço chamado `pedidos-kafka`, com o endpoint:

```http
POST /pedidos/eventos
```

O pedido era implementar uma API REST que publicasse um evento de pedido como texto no Kafka. O adapter também deveria oferecer publicação com SSL, Avro e Schema Registry, seguindo os dois modos documentados pela skill.

A separação esperada era:

```text
API REST → Caso de uso → Adapter Kafka → Producer
```

Também pedi configurações externalizadas, Maven Wrapper, testes sem broker e documentação das decisões.

Os testes deveriam cobrir sucesso na publicação, tópico ou payload inválido, chave opcional, falha do producer e restauração da flag de interrupção.

O escopo tinha limites claros: sem consumidor, banco de dados, AWS ou infraestrutura remota. Isso ajudou a avaliar se o agente conseguia entregar o que foi solicitado sem acrescentar componentes desnecessários.

Um pedido nesse formato pode ser escrito assim:

```text
Use a skill java-spring instalada neste projeto.
Leia as referências aplicáveis, incluindo references/kafka.md.

Crie uma API POST /pedidos/eventos que publique eventos como texto Kafka.
Implemente também publicação SSL com Avro e Schema Registry no adapter.

Externalize as configurações, inclua testes sem broker,
execute a validação e documente as decisões e limitações.
```

As convenções de arquitetura ficam na skill. O prompt concentra o objetivo daquela implementação.

## Os resultados registrados

Os exemplos desenvolvidos com Codex e Devin estão disponíveis em `resultado-skill/codex/` e `resultado-skill/devin/`. Ambos partiram do mesmo cenário de aplicação de pedidos com Kafka, orientado pela skill java-spring.

Os registros de validação mostram os seguintes resultados:

| Ferramenta | Resultado registrado | Referência da validação |
| --- | --- | --- |
| Codex | Validação aprovada, com 15 testes e nenhuma falha, erro ou teste ignorado. | Resumo automatizado de 12/09/2026. |
| Devin | Build aprovado, com 16 testes e nenhuma falha, erro ou teste ignorado. | `DECISOES.md` do projeto, com validação em 14/09/2026. |

O exemplo do Devin inclui API REST, caso de uso, adapter com os dois modos de publicação Kafka, schema Avro, configurações externalizadas e Maven Wrapper. Os 16 testes documentados cobrem o adapter, a configuração Kafka, o caso de uso e o controller REST.

Os resultados foram registrados em momentos diferentes. A tabela reúne as validações aprovadas documentadas para cada exemplo; as quantidades de testes, por si só, não indicam superioridade de uma implementação.

Na execução do Codex, a etapa de geração levou aproximadamente **6 minutos**, e a validação externa levou mais **4 minutos e 34 segundos**.

Isso fornece uma referência concreta de tempo para aquela execução. Entretanto, não fiz uma execução equivalente sem a skill, então esses números não demonstram, sozinhos, quanto tempo ela economizou.

Para avaliar as entregas, considerei o código gerado e os registros de validação. Além do build aprovado, é necessário revisar se a implementação segue os padrões definidos pela skill.

Essas execuções não constituem um ranking de modelos. As configurações pessoais dos CLIs continuavam ativas, e não houve um ambiente isolado de benchmark.

## O que os testes demonstraram

Os resultados aprovados mostram que foi possível gerar projetos que compilaram e passaram pelos testes previstos para o cenário.

A validação incluiu comportamentos relevantes da aplicação, como publicação, validação de entrada e tratamento de falhas.

Ainda assim, testes sem broker não comprovam a integração Kafka de ponta a ponta. Eles não validam handshake TLS, acesso real ao Schema Registry ou entrega de mensagens em um ambiente implantado.

Para mim, isso faz parte de uma boa entrega com IA: apresentar o resultado junto com o alcance da validação. A própria skill exige que o agente informe decisões, comandos executados e limitações.

## Como uma skill pode acelerar o desenvolvimento

O ganho esperado começa pela redução de decisões repetidas.

Quando arquitetura, convenções e critérios de teste já estão documentados, o pedido pode se concentrar na funcionalidade. Há menos necessidade de explicar novamente onde colocar cada classe ou como tratar cada categoria de erro.

A skill também orienta o agente a inspecionar o projeto antes de modificar integrações Kafka. Biblioteca, serialização, transporte e semântica de entrega devem seguir o padrão existente, salvo uma solicitação explícita de mudança.

Isso pode reduzir retrabalho durante a revisão: a IA recebe antecipadamente critérios que, de outra forma, apareceriam depois da primeira implementação.

No meu experimento, os exemplos de Codex e Devin têm registros de validação aprovada. A medição do ganho de produtividade em relação a um processo sem skill ainda fica como próximo passo.

## E a economia de tokens?

A economia potencial está em evitar instruções repetidas e rodadas de correção.

Sem uma referência reutilizável, cada tarefa pode carregar novamente uma descrição extensa da arquitetura, dos padrões de código e das regras de integração. Com a skill disponível, o prompt pode apontar para essas orientações e detalhar apenas o trabalho atual.

Mas existe uma distinção importante: a skill também consome tokens quando seus arquivos são lidos.

Um prompt menor não significa automaticamente menor consumo total. O benefício depende do conteúdo consultado, do comportamento da ferramenta e de quantas correções são evitadas.

Neste experimento, não registrei uma comparação de tokens com e sem a skill. Portanto, trato a economia como um benefício potencial, ainda sem percentual comprovado.

Para medir isso, pretendo comparar tarefas equivalentes e observar consumo total, duração, quantidade de intervenções e aderência ao padrão esperado.

## Fazer a IA desenvolver no meu estilo

Essa é a parte que mais me interessa.

Meu estilo de desenvolvimento aparece em decisões recorrentes: responsabilidades bem separadas, configurações fora do código, falhas explícitas, testes de comportamento e respeito às convenções do projeto.

Ao escrever essas decisões na skill, torno meus critérios acessíveis ao agente e também à equipe.

Isso ajuda a transformar preferências que antes dependiam de explicações durante a revisão em orientações que podem ser consultadas desde o início da tarefa.

A revisão continua necessária. A diferença é que passa a existir uma referência compartilhada para avaliar a implementação e ajustar a própria skill quando uma instrução estiver ambígua ou incompleta.

## Vou compartilhar o repositório

Vou disponibilizar o repositório com a skill, suas referências e os exemplos gerados, para que outras pessoas possam experimentar e adaptar os padrões ao próprio contexto.

**Repositório: [inserir o link antes da publicação]**

O melhor ponto de partida é ler o `SKILL.md`, conferir as referências e observar como as instruções aparecem no código e nos testes.

Minha experiência com esse exemplo mostrou que uma skill pode transformar convenções de desenvolvimento em um recurso reutilizável para trabalhar com IA. O próximo passo é medir melhor o impacto em tempo e tokens, mantendo a mesma atenção à qualidade e à validação.

Quais decisões você sempre precisa repetir quando pede à IA para desenvolver uma funcionalidade? Elas podem ser o começo da sua própria skill.
