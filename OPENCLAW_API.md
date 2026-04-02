# OpenClaw API Reference

Полная документация по API OpenClaw для разработки мобильного (Android) клиента.

**Репозиторий:** [github.com/openclaw/openclaw](https://github.com/openclaw/openclaw)
**Документация:** [docs.openclaw.ai](https://docs.openclaw.ai)

---

## Содержание

1. [Архитектура](#архитектура)
2. [Подключение (WebSocket)](#подключение-websocket)
3. [Формат фреймов](#формат-фреймов)
4. [RPC-методы](#rpc-методы)
   - [Сессии](#сессии)
   - [Чат](#чат)
   - [Агенты](#агенты)
   - [Модели](#модели)
   - [Навыки и инструменты](#навыки-и-инструменты)
   - [Text-to-Speech](#text-to-speech)
   - [Каналы](#каналы)
   - [Утверждение выполнения](#утверждение-выполнения)
   - [Устройства (Pairing)](#устройства-pairing)
   - [Ноды (Mobile Node)](#ноды-mobile-node)
   - [Cron-задачи](#cron-задачи)
   - [Использование и биллинг](#использование-и-биллинг)
   - [Вызов агента](#вызов-агента)
   - [Конфигурация](#конфигурация)
   - [Прочее](#прочее)
5. [События (Events)](#события-events)
6. [HTTP API](#http-api)
   - [OpenAI-совместимый Chat Completions](#openai-совместимый-chat-completions)
   - [OpenResponses API](#openresponses-api)
   - [Tools Invoke API](#tools-invoke-api)
7. [Аутентификация](#аутентификация)
8. [Стриминг](#стриминг)
9. [Обнаружение сервера (Service Discovery)](#обнаружение-сервера)
10. [Возможности ноды (Node Capabilities)](#возможности-ноды)
11. [Сценарии для Android-клиента](#сценарии-для-android-клиента)

---

## Архитектура

```
Messaging Channels  ──>  Gateway (ws://127.0.0.1:18789)  ──>  AI Agent Runtime
                                |
                   CLI / WebChat / macOS App / Mobile
```

- **Порт по умолчанию:** `18789` (HTTP + WebSocket мультиплексированы на одном порту)
- **Bind по умолчанию:** `127.0.0.1` (loopback)
- **Протокол:** WebSocket с JSON text frames (control plane) + HTTP REST (OpenAI-compatible endpoints)

---

## Подключение (WebSocket)

URL: `ws://<host>:18789` (или `wss://` при TLS)

### Трёхшаговый хендшейк

**Шаг 1 -- Server Challenge (сервер -> клиент):**
```json
{
  "type": "event",
  "event": "connect.challenge",
  "payload": {
    "nonce": "random-server-nonce",
    "ts": 1737264000000
  }
}
```

**Шаг 2 -- Connect Request (клиент -> сервер):**
```json
{
  "type": "req",
  "id": "unique-request-id",
  "method": "connect",
  "params": {
    "minProtocol": 3,
    "maxProtocol": 3,
    "client": {
      "id": "android-node",
      "version": "1.0.0",
      "platform": "android",
      "mode": "node",
      "displayName": "My Android Phone",
      "deviceFamily": "android"
    },
    "role": "node",
    "scopes": [],
    "caps": ["camera", "canvas", "screen", "location", "voice"],
    "commands": ["camera.snap", "canvas.navigate", "screen.record", "location.get"],
    "permissions": {
      "camera.capture": true,
      "screen.record": false
    },
    "auth": {
      "token": "...",
      "deviceToken": "..."
    },
    "locale": "ru-RU",
    "userAgent": "openclaw-android/1.0.0",
    "device": {
      "id": "device_fingerprint",
      "publicKey": "...",
      "signature": "...",
      "signedAt": 1737264000000,
      "nonce": "random-server-nonce"
    }
  }
}
```

> **`role`** может быть:
> - `"operator"` -- управление (UI чата, настройки, сессии) с `scopes`: `["operator.read", "operator.write"]`
> - `"node"` -- устройство-исполнитель (камера, GPS, и т.д.) с `caps`, `commands`, `permissions`

**Шаг 3 -- Hello-OK (сервер -> клиент):**
```json
{
  "type": "res",
  "id": "unique-request-id",
  "ok": true,
  "payload": {
    "type": "hello-ok",
    "protocol": 3,
    "policy": {
      "tickIntervalMs": 15000,
      "maxPayload": 1048576,
      "bufferLimits": {}
    },
    "auth": {
      "deviceToken": "persisted-device-token",
      "role": "operator",
      "scopes": ["operator.read", "operator.write"]
    }
  }
}
```

> Полученный `deviceToken` нужно сохранить для последующих переподключений.

---

## Формат фреймов

Все сообщения -- JSON text frames.

### Request (клиент -> сервер)
```json
{
  "type": "req",
  "id": "unique-id",
  "method": "method.name",
  "params": {}
}
```

### Response (сервер -> клиент)
```json
{
  "type": "res",
  "id": "unique-id",
  "ok": true,
  "payload": {}
}
```

### Response Error
```json
{
  "type": "res",
  "id": "unique-id",
  "ok": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": {},
    "retryable": false,
    "retryDelayMs": 1000
  }
}
```

### Event (сервер -> клиент)
```json
{
  "type": "event",
  "event": "event.name",
  "payload": {},
  "seq": 42,
  "stateVersion": 7
}
```

---

## RPC-методы

### Health & Status

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `health` | Проверка состояния Gateway | -- (без аутентификации) |
| `status` | Полный статус системы | -- (sensitive data требует `operator.admin`) |
| `doctor.memory.status` | Диагностика памяти | -- |
| `logs.tail` | Хвост лог-файла | `cursor`, `limit` (default 500, max 5000) |

---

### Сессии

Основные методы для построения UI чата.

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `sessions.list` | Список сессий | `limit`, `activeMinutes`, `includeGlobal`, `includeUnknown`, `includeDerivedTitles`, `includeLastMessage`, `label`, `spawnedBy`, `agentId`, `search` |
| `sessions.create` | Создать сессию | `key`, `agentId`, `label`, `model`, `parentSessionKey`, `task`, `message` |
| `sessions.send` | Отправить сообщение | `key`, `message`, `thinking`, `attachments[]`, `timeoutMs`, `idempotencyKey` |
| `sessions.abort` | Прервать активный run | `key`, `runId` (опционально) |
| `sessions.patch` | Обновить настройки сессии | `label`, `thinkingLevel`, `fastMode`, `verboseLevel`, `reasoningLevel`, `responseUsage` (`off`/`tokens`/`full`/`on`) |
| `sessions.reset` | Сбросить сессию | `key`, `reason` (`new`/`reset`) |
| `sessions.delete` | Удалить сессию | `key`, `deleteTranscript`, `emitLifecycleHooks` |
| `sessions.compact` | Компактировать транскрипт | `key`, `maxLines` |
| `sessions.preview` | Предпросмотр сессий | `keys[]` (min 1), `limit`, `maxChars` |
| `sessions.subscribe` | Подписаться на изменения сессий | -- |
| `sessions.unsubscribe` | Отписаться от изменений | -- |
| `sessions.messages.subscribe` | Подписаться на сообщения сессии | `key` |
| `sessions.messages.unsubscribe` | Отписаться от сообщений | `key` |

**Пример: создание сессии и отправка сообщения:**
```json
// 1. Создать
{"type":"req","id":"1","method":"sessions.create","params":{"agentId":"default","label":"Android Chat"}}

// 2. Подписаться на сообщения
{"type":"req","id":"2","method":"sessions.messages.subscribe","params":{"key":"session-key-from-create"}}

// 3. Отправить сообщение
{"type":"req","id":"3","method":"sessions.send","params":{"key":"session-key","message":"Привет!"}}

// 4. Получать стриминг через events:
// <- {"type":"event","event":"session.message","payload":{...},"seq":1}
// <- {"type":"event","event":"agent","payload":{...},"seq":2}
```

---

### Чат

Webchat/operator интерфейс.

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `chat.history` | История чата | настраиваемые лимиты |
| `chat.send` | Отправить сообщение | сообщение, вложения, timestamps, media |
| `chat.abort` | Прервать активный run | -- (проверяется по device/connection identity) |

---

### Агенты

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `agents.list` | Список настроенных агентов | -- |
| `agents.create` | Создать агента | workspace init + identity file setup |
| `agents.update` | Обновить агента | `name`, `workspace`, `model`, `avatar` |
| `agents.delete` | Удалить агента | optional trash for files |
| `agents.files.list` | Файлы воркспейса агента | возвращает size, mtime |
| `agents.files.get` | Содержимое файла | с валидацией пути |
| `agents.files.set` | Записать файл | whitelisted: `agents.json`, `soul.json`, `tools.json`, `identity.md`, `user.md`, `heartbeat.json`, `bootstrap.md`, `memory.md`, `memory.alt.md` |

---

### Модели

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `models.list` | Список доступных моделей | фильтруется по allowed model set |

---

### Навыки и инструменты

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `skills.status` | Статус навыков агента | -- |
| `skills.bins` | Список бинарных зависимостей | -- |
| `skills.install` | Установить навык | из ClawHub или direct |
| `skills.update` | Обновить навык | toggle enabled, API keys, env vars |
| `tools.catalog` | Каталог инструментов | группировка по source (требует `operator.read`) |
| `tools.effective` | Эффективный набор инструментов сессии | `sessionKey` (требует `operator.read`) |

---

### Text-to-Speech

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `tts.status` | Статус TTS | возвращает enabled, active provider, fallbacks |
| `tts.providers` | Список провайдеров | config, models, voices |
| `tts.enable` | Включить TTS | -- |
| `tts.disable` | Выключить TTS | -- |
| `tts.convert` | Текст -> речь | `text`, `channel` (опц.); возвращает audio file path, provider, format, voice info |
| `tts.setProvider` | Сменить провайдера | provider name |

---

### Каналы

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `channels.status` | Статус каналов | runtime snapshots, accounts, optional probe |
| `channels.logout` | Выйти из аккаунта канала | -- |

---

### Утверждение выполнения

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `exec.approvals.get` | Получить настройки утверждений | -- |
| `exec.approvals.set` | Установить настройки утверждений | -- |
| `exec.approvals.node.get` | Утверждения для нод | -- |
| `exec.approvals.node.set` | Установить утверждения для нод | -- |
| `exec.approval.request` | Запросить утверждение | `systemRunPlan`: `{argv, cwd, rawCommand}` |
| `exec.approval.waitDecision` | Ожидать решения | -- |
| `exec.approval.resolve` | Утвердить/отклонить | требует `operator.approvals` |
| `plugin.approval.request` | Запрос утверждения плагина | -- |
| `plugin.approval.waitDecision` | Ожидать решения по плагину | -- |
| `plugin.approval.resolve` | Утвердить/отклонить плагин | -- |

---

### Устройства (Pairing)

Критически важно для мобильного клиента.

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `device.pair.list` | Список ожидающих и сопряжённых устройств | токены редактированы |
| `device.pair.approve` | Одобрить сопряжение | транслирует `device.pair.resolved` |
| `device.pair.reject` | Отклонить сопряжение | -- |
| `device.pair.remove` | Удалить устройство | отключает CLI-клиенты |
| `device.token.rotate` | Ротация токена | требует scope, отключает старые подключения |
| `device.token.revoke` | Отозвать токен | по роли |

---

### Ноды (Mobile Node)

Методы для управления мобильным устройством как нодой-исполнителем.

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `node.pair.request` | Запрос на сопряжение ноды | идемпотентный, транслирует `node.pair.requested` |
| `node.pair.list` | Список нод | pending + paired |
| `node.pair.approve` | Одобрить ноду | выдаёт свежий токен |
| `node.pair.reject` | Отклонить ноду | -- |
| `node.pair.verify` | Проверить nodeId + token | -- |
| `node.rename` | Переименовать ноду | display name |
| `node.list` | Список нод (paired + connected) | -- |
| `node.describe` | Описание конкретной ноды | -- |
| `node.invoke` | Выполнить команду на ноде | с APNS wake для iOS, pending queue |
| `node.pending.drain` | Очистить очередь | -- |
| `node.pending.enqueue` | Поставить в очередь | -- |
| `node.pending.pull` | Получить задачи (сторона ноды) | -- |
| `node.pending.ack` | Подтвердить выполнение | -- |
| `node.invoke.result` | Вернуть результат вызова | -- |
| `node.event` | Отправить событие ноды | -- |
| `node.canvas.capability.refresh` | Обновить canvas capability | -- |

---

### Cron-задачи

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `cron.list` | Список задач | `limit`, `offset`, `query`, `enabled`, `sort` |
| `cron.status` | Статус cron-системы | -- |
| `cron.add` | Создать задачу | schedule: `at`/`every`/`cron`; payload: `systemEvent`/`agentTurn`; delivery: `none`/`announce`/`webhook`; session: `main`/`isolated`/`current`/`session:+...` |
| `cron.update` | Обновить задачу | -- |
| `cron.remove` | Удалить задачу | -- |
| `cron.run` | Запустить вручную | mode: `due`/`force` |
| `cron.runs` | История запусков | с пагинацией |

---

### Использование и биллинг

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `usage.status` | Сводка по использованию провайдеров | -- |
| `usage.cost` | Стоимость за период | `startDate`/`endDate` или `days`, timezone: `utc`/`gateway`/specific |
| `sessions.usage` | Использование по модели/провайдеру/агенту/каналу | -- |
| `sessions.usage.timeseries` | Тайм-серия использования сессии | max 200 точек |
| `sessions.usage.logs` | Логи использования сессии | max 1000 записей |

---

### Вызов агента

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `agent` | Вызвать агента | `message`, `model`, `to`, `replyTo`, `channel`, session management, `thinking`, `attachments`, `timeout`, delivery modes, `lane`, system prompt extension |
| `agent.identity.get` | Получить identity агента | `agentId`, `sessionKey`; возвращает `agentId`, `name`, `avatar`, `emoji` |
| `agent.wait` | Ждать завершения run | `runId`, `timeoutMs` |

---

### Конфигурация

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `config.get` | Получить конфиг | -- |
| `config.set` | Установить конфиг | `raw`, `baseHash` (опц.) |
| `config.apply` | Применить конфиг | `raw`, `baseHash`, `sessionKey`, `note`, `restartDelayMs` |
| `config.patch` | Патч конфига | аналогично apply |
| `config.schema` | JSON-схема конфига + UI hints | -- |
| `config.schema.lookup` | Схема для конкретного пути | path |

---

### Прочее

| Метод | Описание |
|-------|----------|
| `send` | Отправить сообщение (recipient, message/media, channel/account, thread, sessionKey, idempotencyKey) |
| `wizard.start` / `wizard.next` / `wizard.cancel` / `wizard.status` | Setup wizard flow |
| `talk.config` / `talk.speak` / `talk.mode` | Голосовой режим |
| `update.run` | Запустить обновление |
| `voicewake.get` / `voicewake.set` | Конфигурация голосового пробуждения |
| `secrets.reload` / `secrets.resolve` | Управление секретами |
| `last-heartbeat` / `set-heartbeats` | Отслеживание heartbeats |
| `wake` | Разбудить gateway (mode: `now`/`next-heartbeat`, text) |
| `gateway.identity.get` | Identity gateway |
| `system-presence` | Присутствие устройств |
| `system-event` | Системное событие |

---

## События (Events)

События отправляются сервером клиенту через WebSocket.

| Событие | Описание |
|---------|----------|
| `connect.challenge` | Challenge для аутентификации при подключении |
| `agent` | Потоковые события от агента (runId, seq, stream, timestamp, data) |
| `chat` | Сообщения чата |
| `session.message` | Обновления сообщений сессии |
| `session.tool` | Выполнение инструментов в сессии |
| `sessions.changed` | Изменения списка/метаданных сессий |
| `presence` | Изменения присутствия устройств |
| `tick` | Периодический heartbeat (только timestamp) |
| `talk.mode` | Изменения голосового режима |
| `shutdown` | Сервер выключается (reason + optional restart duration) |
| `health` | Изменения состояния здоровья |
| `heartbeat` | Heartbeat-события |
| `cron` | События cron-задач |
| `node.pair.requested` | Новый запрос на сопряжение ноды |
| `node.pair.resolved` | Сопряжение ноды одобрено/отклонено |
| `node.invoke.request` | Запрос на выполнение команды на ноде |
| `device.pair.requested` | Новый запрос на сопряжение устройства |
| `device.pair.resolved` | Сопряжение устройства одобрено/отклонено |
| `voicewake.changed` | Изменение конфигурации голосового пробуждения |
| `exec.approval.requested` | Требуется утверждение выполнения |
| `exec.approval.resolved` | Утверждение выполнения решено |
| `plugin.approval.requested` | Требуется утверждение плагина |
| `plugin.approval.resolved` | Утверждение плагина решено |

---

## HTTP API

Все HTTP-эндпоинты работают на том же порту, что и WebSocket (по умолчанию `18789`).

### OpenAI-совместимый Chat Completions

> **По умолчанию отключен.** Включается в конфигурации gateway.

**Эндпоинты:**

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/v1/chat/completions` | Chat completions (стриминг SSE) |
| GET | `/v1/models` | Список доступных агентов |
| GET | `/v1/models/{id}` | Детали конкретной модели |
| POST | `/v1/embeddings` | Генерация эмбеддингов |

**Аутентификация:** `Authorization: Bearer <token>`

**Модель (поле `model`)** маршрутизирует к агентам:
- `"openclaw"` -- агент по умолчанию
- `"openclaw/default"` -- агент по умолчанию (явно)
- `"openclaw/<agentId>"` -- конкретный агент

**Кастомные заголовки:**

| Заголовок | Описание |
|-----------|----------|
| `x-openclaw-model` | Переопределение модели LLM |
| `x-openclaw-session-key` | Привязка к конкретной сессии |
| `x-openclaw-message-channel` | Канал доставки сообщения |
| `x-openclaw-agent-id` | ID агента |

**Стриминг:** `"stream": true` возвращает `text/event-stream` с `data: <json>` / `data: [DONE]`.

**Персистентность сессии:** Поле `user` используется для стабильного вычисления session key.

**Пример запроса:**
```bash
curl -X POST http://127.0.0.1:18789/v1/chat/completions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openclaw",
    "messages": [{"role": "user", "content": "Привет!"}],
    "stream": true
  }'
```

---

### OpenResponses API

> **По умолчанию отключен.** Включается в конфигурации gateway.

**Эндпоинт:**

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/v1/responses` | Agent-native responses |

**Поддерживаемые типы input:**
- `message` (роли: system, developer, user, assistant)
- `function_call_output`
- `input_image` (base64/URL; JPEG, PNG, GIF, WebP, HEIC, HEIF; max 10MB)
- `input_file` (base64/URL; text, markdown, HTML, CSV, JSON, PDF; max 5MB)

**SSE-события при стриминге:**

| Событие | Описание |
|---------|----------|
| `response.created` | Ответ создан |
| `response.in_progress` | Ответ обрабатывается |
| `response.output_item.added` | Добавлен элемент вывода |
| `response.content_part.added` | Добавлена часть контента |
| `response.output_text.delta` | Дельта текста |
| `response.output_text.done` | Текст завершён |
| `response.completed` | Ответ готов |
| `response.failed` | Ошибка |

**Заголовки:** `Authorization: Bearer <token>`, `x-openclaw-scopes`, `x-openclaw-agent-id`, `x-openclaw-model`, `x-openclaw-session-key`, `x-openclaw-message-channel`.

**Лимиты:** maxBodyBytes 20MB, files 5MB/200k символов, images 10MB, PDF max 4 страницы.

---

### Tools Invoke API

**Эндпоинт:**

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/tools/invoke` | Прямое выполнение инструмента |

**Запрос:**
```json
{
  "tool": "tool_name",
  "action": "json",
  "args": {},
  "sessionKey": "main",
  "dryRun": false
}
```

**Коды ответов:**

| Код | Описание |
|-----|----------|
| 200 | Успех |
| 400 | Невалидный запрос |
| 401 | Не авторизован |
| 404 | Инструмент недоступен |
| 429 | Rate limit |
| 500 | Ошибка выполнения |

**Default deny list:** `exec`, `spawn`, `shell`, `fs_write`, `fs_delete`, `fs_move`, `apply_patch`, `sessions_spawn`, `sessions_send`, `cron`, `gateway`, `nodes`, `whatsapp_login`.

**Max payload:** 2MB.

---

## Аутентификация

### Режимы аутентификации Gateway

| Режим | Описание | Настройка |
|-------|----------|-----------|
| `token` | Bearer токен | `gateway.auth.mode: "token"`, `Authorization: Bearer <token>` или `OPENCLAW_GATEWAY_TOKEN` |
| `password` | Пароль | `gateway.auth.mode: "password"`, `OPENCLAW_GATEWAY_PASSWORD` |
| `trusted-proxy` | Через reverse proxy | `gateway.auth.mode: "trusted-proxy"`, identity в настраиваемом заголовке |
| `none` | Без аутентификации | `gateway.auth.mode: "none"` (не рекомендуется) |

### Device Auth Challenge Protocol

1. Сервер отправляет `connect.challenge` с `nonce`
2. Клиент подписывает payload (v3): platform, device family, device ID, client context, role, scopes, token, server nonce
3. Клиент включает `device.nonce` (совпадает с серверным) + `signature` + `publicKey`
4. Сервер проверяет подпись, свежесть nonce, ключ

### Жизненный цикл Device Token

1. После успешного pairing, gateway выдаёт `deviceToken` в `hello-ok`
2. Клиент **должен сохранить** этот токен для переподключений
3. Токены можно ротировать (`device.token.rotate`) и отзывать (`device.token.revoke`)

### Rate Limiting

- `maxAttempts`: 10 (по умолчанию)
- `windowMs`, `lockoutMs` -- настраиваемые
- Исключение для loopback (опционально)
- HTTP возвращает `429` с `Retry-After`

### Роли и Scopes

**Operator scopes:**
- `operator.read` -- чтение данных
- `operator.write` -- запись данных
- `operator.admin` -- администрирование
- `operator.approvals` -- утверждение exec/plugin
- `operator.pairing` -- управление сопряжением

**Node role:** объявляет `caps`, `commands`, `permissions` вместо operator scopes.

---

## Стриминг

### WebSocket (основной)
Реал-тайм события через event frames:
- `agent` -- потоковые данные от агента
- `session.message` -- обновления сообщений
- `chat` -- сообщения чата

### HTTP SSE (Chat Completions)
`"stream": true` -> `text/event-stream`:
```
data: {"id":"...","object":"chat.completion.chunk","choices":[{"delta":{"content":"Привет"}}]}

data: [DONE]
```

### HTTP SSE (OpenResponses)
SSE-события: `response.created`, `response.in_progress`, `response.output_text.delta`, `response.output_text.done`, `response.completed`, `response.failed`.

### Block streaming в каналы
Завершённые блоки (не token deltas) отправляются через сообщения канала с настраиваемым chunking, coalescing и human-delay pacing.

---

## Обнаружение сервера

### Bonjour / mDNS
- Service type: `_openclaw-gw._tcp`
- TXT records: `role=gateway`, `displayName`, `lanHost`, `gatewayPort`, `tailnetDns`

### Tailscale MagicDNS
Предпочтительный способ для wide-area доступа, надёжнее чем raw IP.

### Ручная настройка / SSH Tunnel
Запасной вариант при отсутствии прямого маршрута.

**Рекомендуемый порядок подключения:**
1. Paired direct endpoint
2. Bonjour LAN
3. Tailnet DNS/IP
4. SSH fallback

---

## Возможности ноды (Node Capabilities)

Android-нода может предоставлять следующие команды через `node.invoke`:

| Команда | Описание | Параметры |
|---------|----------|-----------|
| `camera.snap` | Сделать фото | `facing`: `front`/`back` |
| `camera.clip` | Записать видео | `duration`, `no-audio` |
| `screen.record` | Запись экрана | `duration`, `fps` |
| `canvas.navigate` | Навигация web view по URL | `url` |
| `canvas.snapshot` | Скриншот canvas | `format`: `png`/`jpg`, `maxWidth`, `quality` |
| `canvas.eval` | Выполнить JS в canvas | `script` |
| `canvas.a2ui.push` | Push A2UI контент | content |
| `location.get` | Получить геолокацию | `accuracy`: `precise`, `maxAge` |
| `sms.send` | Отправить SMS | `to`, `message` |
| `device.status` | Статус устройства | -- |
| `notifications.list` | Список уведомлений | -- |
| `photos.latest` | Последние фото | -- |
| `contacts.search` | Поиск контактов | query |
| `calendar.events` | События календаря | -- |
| `callLog.search` | Поиск в журнале звонков | query |
| `system.run` | Выполнить команду | возвращает stdout/stderr/exit code |
| `system.which` | Найти исполняемый файл | -- |

**Пробуждение нод:**
- iOS: APNS (Apple Push Notification Service)
- Android: pending action queue для оффлайн-нод (+ можно интегрировать FCM)

---

## Сценарии для Android-клиента

### Сценарий 1: Оператор (UI чата)

Мобильный клиент как полноценный интерфейс управления:

```
1. Подключиться по WebSocket с role: "operator"
2. scopes: ["operator.read", "operator.write"]
3. Использовать sessions.* для управления сессиями
4. Использовать chat.* для чат-интерфейса
5. Подписаться на events для реал-тайм обновлений
6. agents.list для просмотра/выбора агентов
7. models.list для выбора модели
```

**Поток:**
```
Connect -> sessions.list -> sessions.create -> sessions.messages.subscribe
-> sessions.send -> [получать events: agent, session.message] -> UI
```

### Сценарий 2: Нода (устройство-исполнитель)

Мобильное устройство как capability provider для агента:

```
1. Подключиться по WebSocket с role: "node"
2. Объявить caps: ["camera", "location", "voice"]
3. Объявить commands: ["camera.snap", "location.get", ...]
4. Слушать node.invoke.request events
5. Выполнять команды и возвращать результаты через node.invoke.result
```

### Сценарий 3: Простой HTTP-клиент

Минимальная интеграция через REST API:

```
1. POST /v1/chat/completions с stream: true
2. Читать SSE-поток
3. Не нужен WebSocket, device pairing и т.д.
```

### Сценарий 4: Гибридный (Оператор + Нода)

Два соединения или одно с расширенными capabilities:

```
1. Подключение как operator для UI чата
2. Второе подключение как node для предоставления камеры/GPS
   ИЛИ одно подключение с обоими наборами возможностей
```

---

## Ссылки

| Ресурс | URL |
|--------|-----|
| Репозиторий | [github.com/openclaw/openclaw](https://github.com/openclaw/openclaw) |
| Документация | [docs.openclaw.ai](https://docs.openclaw.ai) |
| Gateway Protocol Schema | [src/gateway/protocol/schema/](https://github.com/openclaw/openclaw/tree/main/src/gateway/protocol/schema) |
| Server Methods List | [src/gateway/server-methods-list.ts](https://github.com/openclaw/openclaw/blob/main/src/gateway/server-methods-list.ts) |
| Agent Schema | [src/gateway/protocol/schema/agent.ts](https://github.com/openclaw/openclaw/blob/main/src/gateway/protocol/schema/agent.ts) |
| Sessions Schema | [src/gateway/protocol/schema/sessions.ts](https://github.com/openclaw/openclaw/blob/main/src/gateway/protocol/schema/sessions.ts) |
| Connect Schema | [src/gateway/protocol/schema/connect.ts](https://github.com/openclaw/openclaw/blob/main/src/gateway/protocol/schema/connect.ts) |
| HTTP Endpoints | [src/gateway/openresponses/](https://github.com/openclaw/openclaw/tree/main/src/gateway/openresponses) |
