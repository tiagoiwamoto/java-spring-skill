# Java Spring Agent Skill

Esta pasta contém a skill `java-spring`, criada no formato aberto Agent Skills para orientar agentes de IA na criação de projetos Java com Spring Boot.

Para instalar e usar no GitHub Copilot, Devin, Claude Code, Gemini CLI ou Codex, consulte o [guia de instruções de uso](INSTRUCOES-DE-USO.md), com comandos e exemplos de tarefas Kafka.

A skill define padrões para:

- Java 25, Spring Boot 4 e Jackson 3;
- arquitetura com `entrypoint`, `core`, `adapter` e `config`;
- records, Bean Validation, MapStruct e injeção por construtor;
- APIs REST e clientes HTTP;
- Spring Cloud AWS 4;
- publicação SNS e SQS standard ou FIFO;
- Kafka seguindo as configurações e o modo existente: texto ou SSL com Avro e Schema Registry;
- inspeção obrigatória de producers e consumers antes de implementar Kafka, distinguindo padrões existentes de decisões novas;
- retry com exponential backoff, full jitter e throttling;
- testes e validação com Maven Wrapper.

## Estrutura atual

```text
skill/
├── README.md
└── java-spring.skill/
    ├── SKILL.md
    └── references/
        ├── architecture.md
        ├── aws-messaging.md
        ├── dependencies.md
        ├── http.md
        └── kafka.md
```

O nome reconhecido pelos agentes vem do campo `name: java-spring` no frontmatter do `SKILL.md`.

## Compatibilidade

A skill segue o formato composto por um diretório com `SKILL.md` e recursos opcionais. Esse formato é baseado no padrão aberto Agent Skills e pode ser usado por ferramentas como:

- OpenAI Codex CLI;
- Gemini CLI;
- GitHub Copilot CLI;
- Claude Code e outros agentes compatíveis com Agent Skills.

Nem toda IA CLI implementa Agent Skills. Quando não houver suporte nativo, o conteúdo do `SKILL.md` pode ser adaptado para o arquivo de instruções próprio da ferramenta.

Referências oficiais:

- [OpenAI — Build skills](https://learn.chatgpt.com/docs/build-skills)
- [Agent Skills specification](https://agentskills.io/)
- [Gemini CLI — Managing Agent Skills](https://geminicli.com/docs/cli/using-agent-skills/)
- [GitHub Copilot — About Agent Skills](https://docs.github.com/en/copilot/concepts/agents/about-agent-skills)

## Preparação para publicação

Para maior clareza em um repositório dedicado, recomenda-se publicar o diretório fonte sem a extensão `.skill`:

```text
java-spring-skills/
├── README.md
├── LICENSE
└── skills/
    └── java-spring/
        ├── SKILL.md
        └── references/
```

A extensão `.skill` pode ser reservada para o pacote compactado distribuído nas releases.

Para preparar essa estrutura a partir deste projeto:

```bash
mkdir -p java-spring-skills/skills
cp -R skill/java-spring.skill java-spring-skills/skills/java-spring
cp skill/README.md java-spring-skills/README.md
```

Adicione também uma licença explícita, como Apache-2.0 ou MIT.

## Publicação no GitHub

Dentro do diretório preparado:

```bash
cd java-spring-skills
git init
git add .
git commit -m "feat: publish java-spring agent skill"
git branch -M main
git remote add origin git@github.com:SEU_USUARIO/java-spring-skills.git
git push -u origin main
```

Substitua `SEU_USUARIO` pelo usuário ou organização que hospedará o repositório.

### Versionamento

Use versionamento semântico:

```bash
git tag -a v1.0.0 -m "Java Spring Skill v1.0.0"
git push origin v1.0.0
```

Cada alteração incompatível deve incrementar a versão major; novos padrões compatíveis incrementam minor; correções incrementam patch.

## Pacote `.skill`

Para gerar um artefato instalável a partir do repositório preparado:

```bash
cd skills
zip -r ../java-spring-v1.0.0.skill java-spring
```

Anexe `java-spring-v1.0.0.skill` à GitHub Release correspondente. Mantenha o repositório Git como fonte oficial, pois nem todos os CLIs instalam diretamente pacotes `.skill`.

## Instalação no Codex CLI

### Escopo pessoal

Skills pessoais ficam disponíveis em todos os projetos do usuário:

```bash
git clone https://github.com/SEU_USUARIO/java-spring-skills.git
mkdir -p ~/.agents/skills
cp -R java-spring-skills/skills/java-spring ~/.agents/skills/
```

O Codex detecta skills pessoais em `~/.agents/skills`.

### Escopo do projeto

Para disponibilizar a skill apenas em um repositório:

```bash
mkdir -p .agents/skills
cp -R /caminho/java-spring-skills/skills/java-spring .agents/skills/
```

Versione `.agents/skills/java-spring` junto ao projeto quando toda a equipe precisar seguir os mesmos padrões.

### Skill Installer

Também é possível solicitar dentro do Codex:

```text
$skill-installer instale a skill do repositório
https://github.com/SEU_USUARIO/java-spring-skills,
caminho skills/java-spring
```

Se a skill não aparecer imediatamente, reinicie a sessão. Para invocá-la explicitamente:

```text
$java-spring crie um microsserviço de pedidos com API REST e PostgreSQL
```

A descrição no `SKILL.md` também permite ativação automática quando a solicitação for compatível.

## Instalação no Gemini CLI

Instalação pelo repositório:

```bash
gemini skills install https://github.com/SEU_USUARIO/java-spring-skills
```

Durante o desenvolvimento, vincule o diretório local:

```bash
gemini skills link ./java-spring-skills/skills/java-spring
```

Para instalação apenas no workspace:

```bash
gemini skills install \
  --scope workspace \
  https://github.com/SEU_USUARIO/java-spring-skills
```

Dentro de uma sessão do Gemini, confirme a descoberta:

```text
/skills list
```

Caso necessário, recarregue:

```text
/skills reload
```

O Gemini também reconhece skills colocadas em `.gemini/skills` ou `.agents/skills`.

## Instalação no GitHub Copilot CLI

Após clonar o repositório, instale o diretório completo para preservar as referências:

```bash
copilot plugins install \
  --skill ./java-spring-skills/skills/java-spring
```

Para o projeto atual:

```bash
mkdir -p .github/skills
cp -R java-spring-skills/skills/java-spring .github/skills/
```

O Copilot CLI também procura skills em:

- `.github/skills`;
- `.agents/skills`;
- `.claude/skills`;
- `~/.copilot/skills`;
- `~/.agents/skills`.

Dentro do Copilot CLI, use `/skills list` para verificar e `/java-spring` para invocação explícita.

## Instalação em outros agentes

Para agentes compatíveis com o padrão, copie o diretório inteiro `java-spring` para a pasta de skills indicada pela ferramenta. Preserve sempre:

```text
java-spring/
├── SKILL.md
└── references/
```

Não distribua apenas o `SKILL.md`, pois ele contém links relativos para os arquivos em `references/`.

Se a ferramenta não implementar Agent Skills:

1. carregue o conteúdo do `SKILL.md` como instrução do agente;
2. disponibilize os arquivos de `references/` no contexto ou knowledge base;
3. adapte a ativação automática às regras da ferramenta;
4. preserve as instruções de segurança e os critérios de compatibilidade de dependências.

## Validação antes de uma release

Confirme que:

- `SKILL.md` começa diretamente com o frontmatter YAML;
- `name` contém apenas letras minúsculas, números e hífens;
- `description` explica claramente quando ativar a skill;
- todos os links relativos existem;
- não há TODOs ou placeholders acidentais;
- scripts, caso sejam adicionados, não contêm credenciais;
- referências não dependem de caminhos locais deste projeto;
- a skill foi testada com solicitações reais de criação de projetos.

Quando o `skill-creator` da OpenAI estiver disponível localmente, execute:

```bash
python3 /caminho/skill-creator/scripts/quick_validate.py \
  skills/java-spring
```

Depois, faça ao menos um teste comportamental em uma pasta temporária:

```text
$java-spring crie um novo projeto de pedidos com Spring Boot,
API REST, PostgreSQL, publicação SNS FIFO e testes
```

Verifique se o projeto gerado compila e se os testes passam.

## Segurança

- Revise qualquer skill antes de instalar de fontes externas.
- Não armazene chaves AWS, tokens, senhas ou endpoints privados.
- Prefira versões marcadas por tags em automações reproduzíveis.
- Revise scripts executáveis antes da instalação.
- Publique checksums dos pacotes `.skill` quando distribuir releases.

Exemplo:

```bash
sha256sum java-spring-v1.0.0.skill \
  > java-spring-v1.0.0.skill.sha256
```

## Atualização

Para uma instalação feita por cópia:

```bash
git -C java-spring-skills pull --ff-only
cp -R java-spring-skills/skills/java-spring ~/.agents/skills/
```

Para instalações gerenciadas pelo Gemini CLI, desinstale e instale novamente quando precisar trocar de versão:

```bash
gemini skills uninstall java-spring
gemini skills install https://github.com/SEU_USUARIO/java-spring-skills
```
