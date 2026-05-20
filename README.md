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
```
