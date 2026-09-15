Use a skill java-spring em .agents/skills/java-spring/SKILL.md. Leia as referências aplicáveis, obrigatoriamente references/kafka.md.
Trabalhe apenas neste diretório. Não leia resultados de outras IAs. Não altere .agents nem _teste-skill. Não faça commit, push ou deploy.

Crie nesta pasta um projeto Maven Java 25 / Spring Boot 4 chamado
pedidos-kafka, pacote br.com.exemplo.pedidos, seguindo a skill java-spring.
Implemente API REST POST /pedidos/eventos que publica um evento de pedido
como texto Kafka, e um adapter que também ofereça publicação SSL com Avro
e Schema Registry seguindo os dois modos documentados pela skill.
Externalize as configurações. Use um registro Avro compatível no modo Avro.
Inclua testes unitários sem broker cobrindo sucesso, tópico/payload inválido,
chave opcional, falha do producer e restauração de interrupção.
Não adicione consumidor, banco de dados, AWS ou infraestrutura remota.
Inclua pom.xml, Maven Wrapper completo, src/main e src/test diretamente
nesta pasta, sem criar outro diretório de projeto intermediário.
Execute os testes e corrija as falhas que puder resolver.
Documente em DECISOES.md os padrões aplicados, arquivos da skill consultados,
propriedades exigidas e limitações da validação. Não invente resultados.
