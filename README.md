# Budgetintg API
Projeto desenvolvido como parte do Desafio de Projeto do módulo **Spring AI** da [trilha Java Spring Boot da DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track). A proposta original do desafio era explorar os recursos do Spring AI (chat, function calling, transcrição de áudio e text-to-speech); a partir dela, construí um assistente financeiro pessoal controlado por voz

## O que o projeto faz
Você manda um áudio dizendo algo como "gastei 50 reais no mercado", e a API:
1. Transcreve o áudio para texto.
2. Entende a intenção (registrar um gasto, consultar o total, ou consultar por categoria)
3. Executa a ação correspondente no banco de dados
4. Responde em áudio, de forma natural, confirmando o que foi dito.

Também é possível interagir sem voz, direto via REST (JSON), para quem prefere ou para automações.

## Tecnologias Usadas
- **Java 21 + Spring 4**
- **Spring AI** (`2.0.0-M4`) integrado ao Google GenAI (Gemini) - chat, function calling, transcrição de áudio e text-to-speech
- **MySQL** (via Docker Compose, com start automático pelo Spring Boot)
- **Spring Data JPA** para persistência
- **JUnit 5, Mockito** e **AssertJ** para testes
- **GitHub Actions** para integração contínua (CI)
- **Lombok**

## Como executar a aplicação

### Pré-requisitos
- Java 21
- Docker (pro banco de dados)
- Uma chave de API do Google GenAI ([AI Studio ](https://aistudio.google.com) ou [Console Cloud Google](https://console.cloud.google.com))

### Passo a Passo
```bash
export GOOGLE_GENAI_API_KEY=sua_chave_aqui
./gradlew bootRun
```
O Spring Boot sobe o container do MySQL automaticamente (via `spring-boot-docker-compose`), não precisa rodar `docker compose up` manualmente. A aplicação fica disponível em `http://localhost:8080`.

> **Sobre a cota da API**: O tier gratuito do Google GenAI tem limites baixos de requisições (ás vezes só ~20/dia por modelo), com reset às 04:00 no horário de Brasília. Se você receber o erro `429`, é isso - não é bug. Erros `503` também podem ocorrer por instabilidade momentânea do lado do Google; o Spring AI já faz retry automático nesses casos.

## Como testar o fluxo principal

### Via voz (fluxo completo)

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@caminho/do/audio.ogg;type=audio/ogg" \
  --max-time 60 \
  --output response.ogg
```

Grave um áudio dizendo, por exemplo, "gastei 30 reais no mercado" ou "quanto eu já gastei no total", e ouça `response.ogg`

### Via REST puro (sem custo de API)

```bash
curl -X POST http://localhost:8080/transactions/ai \
     --url 'http://example.com'\
     --output './path/to/file'
     
curl http://localhost:8080/transactions/total
```

### Rodando os testes automatizados

```bash
./gradlew test
```

Roda todos os testes unitários (domínio, use cases, tratamento de erros HTTP) - rápido, sem rede, sem gastar cota de API.
As classes de integração (sufixo `IT`) são excluídas desse comando de propósito dependem de chamadas reais ao Google GenAI; 
rodam apenas manualmente, se necessário. O mesmo `./gradlew test` roda automaticamente via GitHub Actions em todo push/PR 
para a `main`

## Melhorias implementadas

Partindo da versão base do desafio, evoluí o projeto em etapas incremente, cada uma com sua própria branch e Pull Request:

1. **Refatoração da camada de áudio** - a lógica e síntese de voz estava duplicada entre três controllers. Extraí para dois 
serviços dedicados (`AudioTranscriptionService`, `AudioSpeechService`) em `infrastructure/ai`, e movi configurações hardcoded
(prompt de transcrição, voz do TTS) para arquivos de recurso e `application.properties`.
2. **Novos tipos de consulta financeira** - adicional a capacidade de somar gastos (por categoria e total), construída em quatro 
camadas sucessívas: repositório (query JPQL com `COALESCE` para evitar `NULL`), use case, endpoint REST (`GET /transactions/total`, 
`GET /transactions/{category}/total`), e por fim como *tool* de IA, pra que o assistente também responde isso por voz.
3. **Melhoria no tom da respostas da IA** - ajustei o prompt de sistema para que as respostas sejam mais naturais e conversacionais, 
e os valores monetários sejam escritos por extenso (já que são convertidos em áudio via TTS - números formatados como "R$ 50,20" 
soam estranhos quando falados).
4. **Validação de domínio** - o objeto `Transaction` agora garante suas próprias invariantes (valor positivo, descrição não vazia, 
categoria obrigatória) direto no construtor, seguindo o príncipio de *always-valid domain model* do DDD. Isso protege tanto o fluxo REST 
quanto a tool da IA, já que os dois convergem pro mesmo construtor. Um `@RestControllerAdvice` trata essas validações no REST, devolvendo 
`400` com mensagem clara em vez de `500` genérico.
5. **Testes automatizadas e CI** - adicionei testes unitários cobrindo os fluxos princípais (validação de domínio, os quatro use cases, e 
o roteamento HTTP de erros), todos mockados e sem dependência de rede ou banco. Configurei um workflow do GitHub que roda esses testes 
automaticamente em todo push/PR.

## O que aprendi durante o desafio
- **Function calling na prática**: como o Spring AI conecta a saída de um LLM a métodos Java reais via `@Tool`, incluindo como exceções 
lançadas dentro de uma tool são automaticamente devolvidas ao modelo como contexto, sem precisar de tratamento manual.
- **Domain-Driven Design aplicado**: entender por que uma entidade deve proteger seus próprios invariantes (não confiar só na camada de aplicação 
ou na validação de entrada HTTP), especialmente relevante aqui porque a mesma entidade é alcançada por dois caminhos de entrada diferentes (REST e IA).
- **Separar testes por custo e propósito**: a diferença prática entre testes unitários (rápidos, mockados, seguros pra rodar em qualquer push) e testes 
de integração (lentos, com custo real de API, fora do pipeline automático) — e como isso molda o desenho do CI.
- **Debugar sistemas distribuídos com dependência externa**: diferenciar, na prática, entre erro de cota (`429`), erro de validação (`400`), instabilidade 
do provedor (`503`) e timeout do próprio cliente de teste — cada um exige um diagnóstico e uma solução diferentes.
- **Fluxo de Git incremental**: dividir uma melhoria maior em etapas pequenas e testáveis, cada uma com sua própria branch, commit descritivo e PR, em vez 
de um único commit grande no final.

## Uma nova ideia usando a mesma base técnica

A arquitetura desse projeto (transcrição de voz → interpretação por IA com function calling → persistência → resposta em voz) não é exclusiva de finanças — ela serve como um padrão reutilizável para *qualquer* domínio de registro e consulta por voz. Uma ideia natural de evolução, aproveitando a mesma stack:

### Assistente de Estudos por Voz

Em vez de registrar transações financeiras, a pessoa registraria sessões de estudo por voz: *"estudei 2 horas de Java hoje"*, *"quanto eu já estudei de Spring Boot esse mês?"*. A estrutura de domínio seria quase um espelho da atual:

- `StudySession` (em vez de `Transaction`): assunto, duração, categoria (ex: linguagem, framework, algoritmos), data.
- Tools de IA equivalentes: registrar sessão, listar sessões por assunto, somar horas totais ou por assunto.
- Uma extensão natural: um endpoint que sugere, com base no histórico, quais assuntos estão sendo negligenciados — útil pra quem, como eu, estuda várias tecnologias em paralelo (Java, Python, Rust, Linux) e às vezes perde a noção de onde o tempo está indo.
  Dá pra reaproveitar praticamente toda a infraestrutura já construída aqui — a única mudança real de peso seria o domínio (`StudySession` no lugar de `Transaction`) e o prompt de sistema; toda a arquitetura de transcrição, tool calling e síntese de voz seria reutilizada sem alteração.
 