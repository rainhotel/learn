# ModelMesh Gateway

High-availability OpenAI-compatible AI API gateway built with Java 21 and Spring Boot WebFlux.

## Current Scope

- `POST /v1/chat/completions`
- Non-streaming JSON response
- `stream: true` SSE passthrough
- Local Mock Provider with failure modes
- Weighted model alias routing
- Fallback before streaming starts

## Run

```powershell
mvn.cmd spring-boot:run
```

## Try Non-Streaming

```powershell
curl.exe -X POST http://localhost:8080/v1/chat/completions `
  -H "Content-Type: application/json" `
  -d "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}"
```

## Try Streaming

```powershell
curl.exe -N -X POST http://localhost:8080/v1/chat/completions `
  -H "Content-Type: application/json" `
  -d "{\"model\":\"gpt-4o-mini\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]}"
```

## Mock Failure Modes

The built-in mock endpoint accepts `x-mock-mode`:

- `ok`
- `slow`
- `429`
- `500`
- `stream-drop`

The gateway routing config is in `src/main/resources/application.yml`.
