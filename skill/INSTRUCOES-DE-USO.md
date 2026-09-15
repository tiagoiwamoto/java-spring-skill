# Usar a skill java-spring nos agentes de desenvolvimento

Este guia explica como disponibilizar a [skill java-spring](java-spring.skill/SKILL.md) no GitHub Copilot, Devin, Claude Code, Gemini CLI e Codex CLI/extensão de IDE. Documentação consultada em 12/09/2026. As instruções de Claude e Gemini abaixo se referem às ferramentas de desenvolvimento com acesso ao repositório.

## Preparação

A origem neste repositório é o **diretório** `skill/java-spring.skill/`; ele não é um arquivo compactado. Copie seu conteúdo completo, incluindo `references/`, para uma pasta chamada `java-spring` no projeto em que o agente trabalhará.

Execute os comandos Bash abaixo na raiz do projeto destino. Antes, ajuste a variável para o caminho absoluto da skill original:

```bash
SKILL_ORIGEM='/caminho/para/java-spring-skill/skill/java-spring.skill'
```

Se estiver na raiz deste próprio repositório, use:

```bash
SKILL_ORIGEM="$PWD/skill/java-spring.skill"
```

Os comandos de cópia também atualizam arquivos de uma instalação anterior. Revise alterações locais antes de repeti-los; arquivos removidos da origem precisam ser reconciliados no destino. Mantenha a origem como fonte de manutenção e evite versões diferentes da mesma skill nos diretórios lidos por um agente.

## Caminhos por ferramenta

| Ferramenta | Pasta da skill no projeto | Uso explícito sugerido |
| --- | --- | --- |
| GitHub Copilot | `.github/skills/java-spring/` | Pedir para usar `java-spring` e informar o caminho |
| Devin | `.agents/skills/java-spring/` | Pedir para ler a skill na tarefa |
| Claude Code | `.claude/skills/java-spring/` | `/java-spring` |
| Gemini CLI | `.agents/skills/java-spring/` | Pedir para usar `java-spring` |
| Codex CLI / IDE | `.agents/skills/java-spring/` | `$java-spring` |

As fontes de cada caminho estão nas respectivas seções. Devin, Gemini CLI e Codex podem compartilhar a mesma cópia em `.agents/skills/java-spring`. Copilot também aceita `.agents/skills`: se já usar esse diretório, não precisa criar outra cópia para ele. Para Claude Code, use o caminho próprio indicado acima.

## GitHub Copilot

Instale no projeto:

```bash
mkdir -p .github/skills/java-spring
cp -R "$SKILL_ORIGEM"/. .github/skills/java-spring/
```

Abra o projeto no modo agente do Copilot ou use o Copilot CLI. Para execução remota, disponibilize os arquivos na versão do repositório usada pela sessão.

Exemplo de pedido:

```text
Use a skill java-spring em .github/skills/java-spring/SKILL.md.
Implemente publicação Kafka de pedidos. Leia references/kafka.md antes
de editar e preserve a configuração e o modo de publicação existentes.
```

Se instalou no diretório compartilhado, substitua o caminho por `.agents/skills/java-spring/SKILL.md`. A seleção automática depende da relevância da descrição da skill para a tarefa. [Documentação oficial do GitHub Copilot](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/customize-cloud-agent/add-skills).

## Devin

Instale na raiz do repositório que o Devin utilizará:

```bash
mkdir -p .agents/skills/java-spring
cp -R "$SKILL_ORIGEM"/. .agents/skills/java-spring/
```

Inclua a pasta completa no controle de versão e disponibilize-a no repositório conectado ao Devin. Uma cópia somente na sua máquina não estará disponível para uma sessão remota.

Na tarefa, escreva:

```text
Neste repositório, use .agents/skills/java-spring/SKILL.md.
Antes de implementar o consumidor Kafka, leia references/kafka.md e
inspecione os consumidores e as configurações existentes em src.
Siga o mesmo padrão. Se não houver consumidor, apresente as decisões
novas sem afirmar que já existem na referência.
```

Devin descobre skills nesse diretório nos repositórios conectados. [Documentação oficial do Devin](https://docs.devin.ai/product-guides/skills).

## Claude Code

Instale no projeto:

```bash
mkdir -p .claude/skills/java-spring
cp -R "$SKILL_ORIGEM"/. .claude/skills/java-spring/
```

Abra uma sessão do Claude Code no repositório e envie:

```text
/java-spring implemente publicação Kafka preservando o modo configurado no projeto
```

A skill também pode ser escolhida automaticamente pela descrição. Para uso pessoal entre projetos locais, o destino alternativo é `~/.claude/skills/java-spring/`. Essa instalação pessoal não disponibiliza os arquivos automaticamente a sessões remotas. [Documentação oficial do Claude Code](https://code.claude.com/docs/en/skills).

## Gemini CLI

Use a cópia compartilhada, caso já tenha sido criada para Devin ou Codex. Caso contrário:

```bash
mkdir -p .agents/skills/java-spring
cp -R "$SKILL_ORIGEM"/. .agents/skills/java-spring/
```

Na sessão do Gemini CLI, atualize a descoberta e confira a listagem:

```text
/skills reload
/skills list
```

Depois envie:

```text
Use a skill java-spring para implementar a integração Kafka solicitada.
Leia references/kafka.md e siga a configuração, a serialização e o modo
de consumir ou publicar já adotados neste projeto.
```

O Gemini CLI reconhece `.agents/skills/` como alternativa a `.gemini/skills/`. Se as duas pastas contiverem skills de mesmo nome no mesmo escopo, `.agents/skills/` tem precedência. [Documentação oficial do Gemini CLI](https://geminicli.com/docs/cli/skills/).

## Codex CLI e extensão de IDE

Use a mesma cópia compartilhada de Devin e Gemini CLI, ou instale:

```bash
mkdir -p .agents/skills/java-spring
cp -R "$SKILL_ORIGEM"/. .agents/skills/java-spring/
```

Abra o Codex no projeto. Use `/skills` para localizar a skill ou invoque no prompt:

```text
$java-spring implemente publicação Kafka com o mesmo padrão existente em src
```

O Codex também pode selecionar a skill pela descrição. Se ela não aparecer após a instalação, reinicie a sessão. Para disponibilidade pessoal em projetos locais, use `~/.agents/skills/java-spring/`. [Documentação oficial da OpenAI sobre skills](https://learn.chatgpt.com/docs/build-skills).

## Instrução pronta para tarefas Kafka

Cole este texto junto do pedido em qualquer uma das ferramentas, ajustando o caminho ao destino instalado:

```text
Use a skill java-spring instalada neste projeto. Antes de qualquer mudança
Kafka, leia seu SKILL.md e references/kafka.md. Inspecione src, o build,
as propriedades e os perfis relevantes para identificar como o projeto
consome ou publica mensagens.

Siga sempre o padrão encontrado: biblioteca, beans/qualifiers, transporte,
autenticação, serialização, Schema Registry, tópicos, chaves, retries,
timeouts e semântica de confirmação. Preserve as configurações existentes,
salvo quando eu solicitar explicitamente uma alteração.

A base desta skill contém publicação de texto e publicação SSL com Avro
e Schema Registry, mas não contém consumidor Kafka. Se este projeto
também não tiver consumidor, diferencie as novas decisões dos padrões
já implementados e esclareça apenas escolhas necessárias não resolvidas.

Ao concluir, informe quais arquivos serviram de referência, o que mudou
e quais validações foram executadas.
```

Instalar uma skill a torna disponível para seleção; a instalação sozinha não garante sua ativação em toda tarefa. Para uma tarefa Kafka, use a invocação explícita ou o texto acima.

## Conferir a instalação

No destino escolhido, confirme esta estrutura:

```text
java-spring/
├── SKILL.md
└── references/
    ├── architecture.md
    ├── aws-messaging.md
    ├── dependencies.md
    ├── http.md
    └── kafka.md
```

Faça primeiro um pedido de inspeção, sem alterações:

```text
Use a skill java-spring. Sem editar arquivos, identifique o modo Kafka
existente, os arquivos que o definem e se há consumidor implementado.
```

Confira se o agente leu a skill e `references/kafka.md` e se suas conclusões correspondem ao código. Se não localizar a skill, confira a raiz da sessão, o destino da cópia e a disponibilidade dos arquivos no checkout remoto. Preserve todas as referências ao mover ou atualizar a skill.

## Testar geração com Codex, Gemini e Devin

O script [testar-geracao-skill.py](../scripts/testar-geracao-skill.py) executa os **CLIs locais** das três ferramentas, em sequência, com o mesmo prompt e uma cópia completa da skill. Requer Linux/macOS ou WSL, Python 3, Java 25 e os CLIs instalados e autenticados. Maven Wrapper pode precisar de rede para baixar Maven e dependências. Devin usa o CLI local, não a API de sessões cloud.

Na raiz deste repositório:

```bash
# Inspecionar o cenário e os comandos, sem chamar modelos nem criar resultados:
python3 scripts/testar-geracao-skill.py --dry-run

# Executar geração e testes nas três ferramentas:
python3 scripts/testar-geracao-skill.py

# Executar apenas uma ferramenta:
python3 scripts/testar-geracao-skill.py --ia codex

# Selecionar ferramentas e ajustar o tempo máximo por geração:
python3 scripts/testar-geracao-skill.py --ia gemini devin --timeout 2400
```

O cenário padrão cria um serviço de pedidos com REST, publicação Kafka texto e SSL/Avro, Maven Wrapper e testes sem broker. O código fica diretamente em:

```text
resultado-skill/
├── codex/                  # pom.xml, src/, mvnw, .mvn/...
├── gemini/                 # pom.xml, src/, mvnw, .mvn/...
├── devin/                  # pom.xml, src/, mvnw, .mvn/...
└── resumo-<execucao>.json
```

Cada pasta contém `.agents/skills/java-spring/` e `_teste-skill/` com `prompt.md`, `geracao.log`, `maven.log` (quando o build for executado) e `resultado.json`. Os resultados registram hashes da skill e do prompt para comparação. As configurações pessoais dos CLIs continuam valendo; isto não é um benchmark isolado de modelos.

O executor verifica arquivos gerados, executa `sh ./mvnw -B -ntp clean test` e exige pelo menos um teste não ignorado, sem falhas, nos relatórios Surefire. Retorna `0` se todos os projetos selecionados passarem; `1` se houver falha de geração/build; `2` para argumentos ou pré-requisitos inválidos; `130` para interrupção. Uma IA que falhar não impede a tentativa da próxima. Timeout encerra o grupo de processos e preserva código parcial e logs.

Passar no Maven não prova aderência completa à skill: revise o código, a cobertura dos testes e `DECISOES.md`, especialmente a configuração Kafka. O executor não trata o texto de sucesso do modelo como prova de validação.

Use `--prompt-file arquivo.md` para trocar o cenário, mantendo o contrato de saída Maven na raiz e testes JUnit/Surefire. `--build-timeout` controla o limite do Maven. `--codex-model`, `--gemini-model` e `--devin-model` permitem escolher modelos disponíveis na sua conta; quando omitidos, valem os padrões dos respectivos CLIs.

O script recusa pastas de resultado existentes. Para repetir, mova a pasta da IA para um backup ou use `--output outro-diretorio`. Ele não apaga resultados anteriores. Chamadas reais usam o acesso/cota das suas contas. Os comandos usam `workspace-write`, `auto_edit` e `accept-edits`, respectivamente, sem habilitar modos de aprovação irrestrita. Se um CLI bloquear a sessão por confiança no workspace, autenticação ou permissão de ferramenta, confira `geracao.log`, configure o acesso na ferramenta e repita após preservar a tentativa anterior. O diretório de trabalho não é uma sandbox comum às três ferramentas.

Interfaces usadas: [Codex não interativo](https://learn.chatgpt.com/docs/non-interactive-mode), [Gemini headless](https://geminicli.com/docs/cli/headless/) e [Devin CLI](https://docs.devin.ai/work-with-devin/devin-cli), além do `--help` dos CLIs instalados.

Para verificar o executor com CLIs simulados, sem chamadas pagas:

```bash
python3 -m unittest discover -s scripts/tests -v
```
