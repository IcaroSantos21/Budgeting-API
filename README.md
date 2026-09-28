# Budgeting API

[![Tests](https://github.com/IcaroSantos21/Budgeting-API/actions/workflows/tests.yml/badge.svg)](https://github.com/IcaroSantos21/Budgeting-API/actions/workflows/tests.yml)

Assistente financeiro pessoal que funciona por voz ou por interface web. A pessoa diz algo como "gastei 50 reais no mercado" e o sistema registra o gasto, consulta totais e responde em áudio.

O projeto nasceu do Desafio de Projeto do módulo Spring AI da [trilha Java Spring Boot da DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track), que propõe explorar chat, function calling, transcrição de áudio e text-to-speech. A partir dessa base, foi ampliado com regras de domínio, testes, CI e um front-end em Angular.

## Visão geral

Fluxo por voz:

1. O áudio é enviado para a API e transcrito para texto.
2. A IA interpreta a intenção: registrar um gasto, consultar o total ou consultar o total de uma categoria.
3. A ação correspondente é executada no banco de dados (via function calling).
4. A resposta volta em áudio, em tom de conversa.

O mesmo conjunto de operações também está disponível por REST/JSON, usado pelo front-end e útil para automações.

## Tecnologias

**Back-end**
- Java 21 e Spring Boot 4
- Spring AI `2.0.0-M4` com Google GenAI (Gemini): chat, function calling, transcrição e text-to-speech
- Spring Data JPA e MySQL (Docker Compose, iniciado automaticamente pelo Spring Boot)
- JUnit 5, Mockito e AssertJ
- GitHub Actions para CI
- Lombok

**Front-end**
- Angular 22, standalone components, sem Zone.js (estado com signals)
- Reactive Forms e roteamento por rotas
- Tailwind CSS v4, com tema claro e escuro

## Estrutura do repositório

```
.
├── src/main/java/dio/budgeting
│   ├── application            # casos de uso e inputs
│   ├── domain                 # Transaction, categorias e regras de negócio
│   └── infrastructure
│       ├── ai                 # transcrição e síntese de voz
│       ├── config             # configuração web (CORS)
│       └── persistence        # repositórios JPA e controllers HTTP
├── src/test                   # testes unitários e de integração
├── frontend                   # aplicação Angular
├── API_DOCUMENTATION.md       # referência completa dos endpoints
└── docker-compose.yaml        # MySQL
```

## Como executar

### Pré-requisitos

- Java 21
- Docker (banco de dados)
- Node.js 22.22.3 ou superior (apenas para o front-end)
- Uma chave de API do Google GenAI ([AI Studio](https://aistudio.google.com) ou [Google Cloud Console](https://console.cloud.google.com))

### Back-end

```bash
export GOOGLE_GENAI_API_KEY=sua_chave_aqui
./gradlew bootRun
```

O Spring Boot sobe o container do MySQL sozinho (`spring-boot-docker-compose`), então não é preciso rodar `docker compose up`. A API fica em `http://localhost:8080`.

> **Cota da API:** o tier gratuito do Google GenAI tem limites baixos, às vezes cerca de 20 requisições por dia por modelo, com reset por volta das 04:00 no horário de Brasília. Um erro `429` costuma ser isso, e não um bug. Erros `503` indicam instabilidade momentânea do lado do Google, e o Spring AI já refaz a chamada automaticamente.

### Front-end

Com a API rodando, em outro terminal:

```bash
cd frontend
npm install
npx ng serve
```

A interface abre em `http://localhost:4200`. A URL da API está em `frontend/src/environments/environment.ts`.

O CORS da API libera a origem `http://localhost:4200` para `GET` e `POST` (`infrastructure/config/WebConfig.java`). Para hospedar o front-end em outro endereço, ajuste essa classe.

## Interface web

A navegação é uma barra lateral com quatro rotas:

| Rota | Tela | O que faz |
|---|---|---|
| `/` | Dashboard | Mostra o total gasto |
| `/add` | Adicionar | Formulário para registrar uma transação |
| `/list` | Transações | Lista as transações de uma categoria |
| `/audio` | Áudio | Grava a voz pelo microfone, envia para a API e toca a resposta |

O botão no rodapé da barra lateral alterna entre tema claro e escuro. A escolha fica salva no navegador, e na primeira visita segue a preferência do sistema.

O navegador grava áudio em `webm/opus`, e é esse formato que a tela de áudio envia para `/transactions/ai`. Ao gravar, o navegador pede permissão de microfone.

## API

Referência completa, com exemplos de requisição e resposta, em [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md).

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/transactions` | Cria uma transação |
| `GET` | `/transactions/{category}` | Lista as transações de uma categoria |
| `GET` | `/transactions/total` | Soma de todas as transações |
| `GET` | `/transactions/{category}/total` | Soma por categoria |
| `POST` | `/transactions/ai` | Fluxo por voz (áudio entra, áudio sai) |
| `POST` | `/api/transcribe` | Transcrição de áudio isolada |
| `POST` | `/api/synthesize` | Síntese de voz isolada |

Categorias válidas: `GROCERIES`, `PHARMA` e `AUTO`.

O campo `amount` é um inteiro em **centavos**: `5000` equivale a R$ 50,00.

### Exemplos

Criar e consultar por REST (não consome cota de IA):

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Compras no mercado","category":"GROCERIES","amount":5000}'

curl http://localhost:8080/transactions/GROCERIES
curl http://localhost:8080/transactions/total
```

Fluxo por voz:

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@caminho/do/audio.ogg;type=audio/ogg" \
  --max-time 60 \
  --output response.ogg
```

Grave algo como "gastei 30 reais no mercado" ou "quanto eu já gastei no total" e ouça o `response.ogg`.

## Testes

```bash
./gradlew test
```

Roda os testes unitários (domínio, casos de uso e tratamento de erros HTTP). São rápidos, sem rede e sem gastar cota de API. As classes de integração, com sufixo `IT`, ficam fora desse comando de propósito porque dependem de chamadas reais ao Google GenAI e rodam só manualmente. O mesmo `./gradlew test` roda no GitHub Actions a cada push e pull request para a `main`.

## Decisões de projeto

- **Camada de áudio isolada.** A transcrição e a síntese de voz estavam duplicadas em três controllers. Foram extraídas para `AudioTranscriptionService` e `AudioSpeechService`, em `infrastructure/ai`, e o prompt de transcrição e a voz do TTS saíram do código para arquivos de recurso e `application.properties`.
- **Consultas como tools de IA.** A soma por categoria e a soma total existem em quatro camadas: repositório (JPQL com `COALESCE` para evitar `NULL`), caso de uso, endpoint REST e tool de IA. Assim o assistente responde às mesmas perguntas por voz.
- **Respostas pensadas para áudio.** O prompt de sistema pede tom de conversa e valores por extenso, porque "R$ 50,20" soa estranho quando falado.
- **Domínio sempre válido.** `Transaction` garante suas invariantes no construtor (valor positivo, descrição não vazia, categoria obrigatória). REST e IA chegam pelo mesmo construtor, então os dois ficam protegidos. Um `@RestControllerAdvice` converte essas violações em `400` com mensagem clara.
- **Testes separados por custo.** Unitários rodam em todo push. Integração, que gasta cota de API, roda só sob demanda.
- **Documentação em Markdown.** Foi tentado o Spring REST Docs, mas há uma incompatibilidade binária entre o REST Docs 4.0 (Spring Framework 7 / Boot 4) e o plugin Asciidoctor do Gradle disponível: fixar a versão do AsciidoctorJ para um quebra o outro. A documentação ficou em `API_DOCUMENTATION.md`.
- **Front-end sem Zone.js.** O projeto Angular usa o modelo padrão atual, sem Zone.js. Por isso todo estado que muda depois de uma chamada assíncrona (HTTP, gravação de áudio) é um signal, senão a tela não atualiza.

## Próximos passos

- Layout responsivo (a barra lateral hoje é fixa).
- Total por categoria no dashboard do front-end.
- Reaproveitar a arquitetura em outros domínios de registro e consulta por voz, como um assistente de estudos (`StudySession` no lugar de `Transaction`, com o mesmo fluxo de transcrição, tool calling e síntese de voz).