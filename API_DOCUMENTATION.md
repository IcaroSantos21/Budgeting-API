# Documentação da API

Base URL local: `http://localhost:8080`

Todos os endpoints que recebem ou retornam JSON usam `Content-Type: application/json`, exceto os de áudio, que usam `multipart/form-data` (entrada) ou `audio/ogg` (saída).

---

## Transações

### Criar transação

```
POST /transactions
```

**Corpo da requisição**

```json
{
  "description": "Compras no mercado",
  "category": "GROCERIES",
  "amount": 5000
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `description` | string | sim | Descrição da transação. Não pode ser vazia. |
| `category` | string | sim | Uma das categorias válidas (ver seção [Categorias](#categorias)). |
| `amount` | inteiro | sim | Valor da transação. Deve ser maior que zero. |

**Resposta — `201 Created`**

```json
{
  "id": "a1b2c3d4-...",
  "description": "Compras no mercado",
  "category": "GROCERIES",
  "amount": 5000.0
}
```

**Resposta de erro — `400 Bad Request`** (violação de validação, ex: `amount` igual a zero)

```json
{
  "message": "Valor da transação deve ser maior que zero"
}
```

**Exemplo com curl**

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Compras no mercado","category":"GROCERIES","amount":5000}'
```

---

### Listar transações por categoria

```
GET /transactions/{category}
```

**Parâmetros de path**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `category` | string | Uma das categorias válidas. |

**Resposta — `200 OK`**

```json
[
  {
    "id": "a1b2c3d4-...",
    "description": "Compras no mercado",
    "category": "GROCERIES",
    "amount": 5000.0
  }
]
```

Retorna uma lista vazia (`[]`) se não houver transações na categoria — não é erro.

**Exemplo com curl**

```bash
curl http://localhost:8080/transactions/GROCERIES
```

---

### Somar transações por categoria

```
GET /transactions/{category}/total
```

**Resposta — `200 OK`**

```json
{
  "total": 7000.0
}
```

Retorna `{"total": 0.0}` se não houver transações na categoria.

**Exemplo com curl**

```bash
curl http://localhost:8080/transactions/GROCERIES/total
```

---

### Somar total geral

```
GET /transactions/total
```

**Resposta — `200 OK`**

```json
{
  "total": 15000.0
}
```

**Exemplo com curl**

```bash
curl http://localhost:8080/transactions/total
```

---

## Fluxo de voz

### Interagir por voz (transcrição → IA → ação → resposta em áudio)

```
POST /transactions/ai
```

Envia um áudio contendo um comando em linguagem natural (registrar gasto ou consultar). A API transcreve, decide qual ação tomar (via function calling), executa, e devolve a resposta em áudio.

**Corpo da requisição** — `multipart/form-data`

| Campo | Tipo | Descrição |
|---|---|---|
| `file` | arquivo de áudio | Formato `.ogg` recomendado. |

**Resposta — `200 OK`**

Arquivo de áudio (`audio/ogg`) com a resposta falada.

**Exemplo com curl**

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@audio.ogg;type=audio/ogg" \
  --max-time 60 \
  --output resposta.ogg
```

**Exemplos de comandos por voz suportados**

- "Gastei 50 reais no mercado" → registra uma transação.
- "Quanto eu já gastei no total?" → soma geral.
- "Quanto eu gastei em farmácia?" → soma por categoria.

> **Nota sobre tempo de resposta**: esse endpoint encadeia várias chamadas de IA (transcrição, decisão de tool, geração de resposta, síntese de voz), podendo levar alguns segundos. Se estiver testando com um cliente HTTP como o do IntelliJ, ajuste o timeout de leitura para pelo menos 30-60 segundos.

---

### Transcrever áudio (isolado)

```
POST /api/transcribe
```

Só transcreve o áudio pra texto, sem interpretar comando nem persistir nada. Útil pra testar a integração de transcrição isoladamente.

**Corpo da requisição** — `multipart/form-data`

| Campo | Tipo | Descrição |
|---|---|---|
| `file` | arquivo de áudio | Formato `.ogg` recomendado. |

**Resposta — `200 OK`**

Texto plano com a transcrição.

**Exemplo com curl**

```bash
curl -X POST http://localhost:8080/api/transcribe \
  -F "file=@audio.ogg;type=audio/ogg"
```

---

### Sintetizar voz (isolado)

```
POST /api/synthesize
```

Converte um texto em áudio, sem passar por transcrição nem IA de decisão. Útil pra testar a síntese de voz isoladamente.

**Corpo da requisição**

```json
{
  "text": "Você gastou cinquenta reais no mercado."
}
```

**Resposta — `200 OK`**

Arquivo de áudio (`audio/ogg`).

**Exemplo com curl**

```bash
curl -X POST http://localhost:8080/api/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"Você gastou cinquenta reais no mercado."}' \
  --output audio.ogg
```

---

## Categorias

Os valores válidos para o campo `category` são definidos pelo enum `Category`:

- `GROCERIES`
- `PHARMA`
- `AUTO`

Enviar uma categoria fora dessa lista resulta em erro de deserialização antes mesmo da validação de domínio ser executada.

---

## Erros comuns

| Status | Causa provável |
|---|---|
| `400 Bad Request` | Dados inválidos (ex: `amount` ≤ 0, `description` vazia, categoria inexistente). |
| `429 Too Many Requests` (do lado do Google GenAI, propagado como erro 500 genérico hoje) | Cota da API do Google GenAI excedida. Reseta à meia-noite (horário do Pacífico). |
| `503 Service Unavailable` (idem) | Instabilidade momentânea do lado do Google. O Spring AI já faz retry automático; tente novamente em alguns instantes. |
| Timeout do cliente | O fluxo `/transactions/ai` encadeia múltiplas chamadas de IA e pode passar de 10s. Aumente o timeout do seu cliente HTTP. |