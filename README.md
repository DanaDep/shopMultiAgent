# ShopMultiAgent

A multi-agent shopping assistant built on Micronaut 4 and LangChain4j with AWS Bedrock.

An `OrchestratorService` coordinates three agents:

- **`ResearcherAgent`** — gathers findings on a topic by calling domain tools (`OrderTool`, `ReviewTool`, `ReturnTool`, `RefundTool`) that read from mock data under `src/main/resources/mock/`. Exposes both `research(topic)` and `deepResearch(topic, findings, gaps)` for follow-up rounds.
- **`WriterAgent`** — turns findings into a written report. Exposes `write(findings)` and `revise(findings, previousReport, issues)`.
- **`CriticAgent`** — evaluates a report against the topic and findings on accuracy, completeness, clarity, no-fabrication, and research adequacy. Returns an `Evaluation` whose `issues` are typed as either `RESEARCH` or `WRITING`.

The orchestrator runs a critique loop (up to 3 iterations): after the Writer produces a draft, the Critic evaluates it; if research issues are found, the Researcher does a deep-research pass and the Writer rewrites from the augmented findings; if only writing issues are found, the Writer revises the existing draft. The loop exits as soon as the Critic returns `acceptable: true`.

The chat UI is served as a single static page and talks to the orchestrator over `POST /api/chat`.

## Stack

- Java 21, Micronaut 4.10.11 (Netty)
- LangChain4j on AWS Bedrock (Claude Sonnet 4.6)
- Gradle 8 (Kotlin DSL), Shadow, Micronaut AOT, GraalVM native image

## Run

```bash
# build
./gradlew build

# run (http://localhost:8080)
./gradlew run

```

## Configuration

AWS Bedrock credentials and region are read from the standard AWS environment / config. App settings live in `src/main/resources/application.yml`.

## Observability

The app emits OpenTelemetry traces for the whole agent pipeline: one span per LLM call (custom LangChain4j `ChatModelListener`), per agent step (`@NewSpan` in `OrchestratorService`), and per HTTP request (Micronaut). Spans carry both `gen_ai.*` (OTel GenAI semconv) and OpenInference attributes, so any OTLP-compatible backend can render them.

The app exports to a single stable endpoint — a local OpenTelemetry Collector (`localhost:4318`) — which fans the same spans out to three backends simultaneously (config: `observability/otel-collector-config.yaml`):

| Backend | Where | Notes |
|---|---|---|
| Arize Phoenix | http://localhost:6006 | local container, no auth |
| Langfuse | https://cloud.langfuse.com | EU cloud; needs `LANGFUSE_AUTH` |
| LangSmith | https://eu.smith.langchain.com | EU cloud; needs `LANGSMITH_API_KEY` |

Start the stack (from a shell where the env vars below are set):

```powershell
# Langfuse uses HTTP Basic auth built from the project's public/secret key pair
$env:LANGFUSE_AUTH = "Basic " + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("pk-lf-xxx:sk-lf-xxx"))
$env:LANGSMITH_API_KEY = "lsv2_xxx"

docker compose -f observability/docker-compose.yml up -d
```

Troubleshooting: `docker logs otel-collector` — the `debug` exporter logs every batch received; per-backend export failures (401 = bad credentials, 403 = wrong region, 404 = doubled `/v1/traces` path) appear as `error` entries naming the failing exporter.

The backend comparison and selection criteria are tracked in `docs/adr/001-llm-observability-backend.md`.

## Project layout

```
src/main/java/com/dep/
  Application.java                # entry point
  controllers/ChatController.java
  services/OrchestratorService.java
  agents/                         # ResearcherAgent, WriterAgent, CriticAgent, AgentFactory
  tools/                          # OrderTool, ReviewTool, ReturnTool, RefundTool
  dtos/                           # Finding, ResearchResult, Evaluation, Issue, Order, Review, ...
  enums/IssueType.java            # RESEARCH | WRITING
  configurations/                 # ChatModelConfig, BedrockProperties
src/main/resources/
  application.yml
  mock/                           # orders.json, reviews.json, returns.json, refunds.json
  public/index.html               # chat UI
observability/
  docker-compose.yml              # Phoenix + OTel Collector stack
  otel-collector-config.yaml      # OTLP receiver + fan-out exporters (Phoenix, Langfuse, LangSmith)
```
