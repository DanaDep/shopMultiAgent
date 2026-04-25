# ShopMultiAgent

A multi-agent shopping assistant built on Micronaut 4 and LangChain4j with AWS Bedrock.

An `OrchestratorService` coordinates two agents — a `ResearcherAgent` that gathers findings on a topic and a `WriterAgent` that turns those findings into a written response. The chat UI is served as a single static page and talks to the orchestrator over `POST /api/chat`.

## Stack

- Java 21, Micronaut 4.10.11 (Netty)
- LangChain4j on AWS Bedrock
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
  Application.java            # entry point
  controllers/ChatController.java
  services/OrchestratorService.java
  agents/                     # ResearcherAgent, WriterAgent
  dtos/                       # Finding, ResearchResult
src/main/resources/
  application.yml
  public/index.html           # chat UI
```
