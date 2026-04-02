# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run application
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.dep.ShopMultiAgentTest"

# Build fat JAR (via Shadow plugin)
./gradlew shadowJar

# Build GraalVM native image
./gradlew nativeCompile

# Build Docker image (native)
./gradlew dockerBuildNative
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

**Micronaut 4.10.11** application (Java 21) using **Netty** as the async HTTP server. AI capabilities are provided via **LangChain4j** with **AWS Bedrock** as the LLM backend.

- Framework: Micronaut with AOT compilation enabled (faster startup, GraalVM-friendly)
- Main entry point: `src/main/java/com/dep/Application.java`
- Default package: `com.dep`
- Config: `src/main/resources/application.yml`

**Build system:** Gradle 8 with Kotlin DSL (`build.gradle.kts`). Key plugins:
- `io.micronaut.application` — Micronaut lifecycle, AOT
- `com.gradleup.shadow` — fat JAR packaging
- `io.micronaut.aot` — ahead-of-time optimizations

**AI/Agent layer:** `io.micronaut.langchain4j:micronaut-langchain4j-bedrock` integrates LangChain4j with AWS Bedrock. Annotation processors for LangChain4j (`micronaut-langchain4j-processor`) and serialization (`micronaut-serde-processor`) run at compile time — any new AI service interfaces need to be processed by these.

**Current state:** Skeleton project — no agent logic or HTTP endpoints implemented yet. The foundation (DI, HTTP server, Bedrock wiring) is in place, ready for multi-agent shopping logic to be built on top.
