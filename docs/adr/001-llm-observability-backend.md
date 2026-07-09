# ADR 001: LLM observability backend for trace analysis

- **Status:** Accepted
- **Date:** 2026-06-11

## Context

The application emits vendor-neutral OpenTelemetry traces for its multi-agent pipeline:
LLM-call spans from a custom LangChain4j `ChatModelListener`, agent/pipeline spans via
Micronaut `@NewSpan`, shipped over OTLP. Because the instrumentation is bound to a
standard (OTel/OTLP) rather than a vendor SDK, the backend that stores and renders the
traces is swappable configuration, not an integration.

We currently use Arize Phoenix locally. Before committing to a backend, we compare three
candidates on identical traffic, delivered simultaneously via an OpenTelemetry Collector
fan-out.

**Scope:** this decision covers *trace collection and analysis only*. Evaluation
capabilities (LLM-as-judge, datasets, scoring) are deliberately out of scope and will be
assessed separately (see Follow-ups).

## Candidates

| | Phoenix | Langfuse | LangSmith |
|---|---|---|---|
| Vendor | Arize | Langfuse GmbH | LangChain Inc. |
| License | Open source (ELv2) | Open source (MIT core) | Proprietary SaaS |
| Deployment evaluated | Local Docker | Cloud (EU, Hobby tier) | Cloud (Developer tier) |
| Ingest | OTLP (native dialect: OpenInference) | OTLP (native dialect: `gen_ai.*`) | OTLP (native dialect: `gen_ai.*`) |

## Decision criteria

Scored 1–5 after comparing identical traffic (one pipeline run = 1 trace = 9 spans) in all
three UIs on 2026-06-11.

| Criterion | Phoenix | Langfuse | LangSmith |
|---|---|---|---|
| Trace UX | **4** — clean, fast, great for local debugging | **5** — richest detail (per-span costs, inferred agent graph, loop detection "2/4"), though dense for newcomers | **3.5** — solid tree + waterfall and rolled-up token counts, but fewer details surfaced |
| Rendering fidelity | **5** — native OpenInference (span kinds, input/output panels) | **5** — read *both* dialects: mapped `openinference.span.kind` to GENERATION/AGENT/CHAIN types and computed costs we never sent | **4** — tokens/model/latency rendered fine, but ignores span kind |
| Data residency | **5** — traces never leave our infrastructure | **3** — EU cloud (GDPR-friendly) on vendor infra; open-source self-host exists as an escape hatch | **2** — EU cloud, but self-hosting is enterprise-tier only; vendor cloud is the only option at our tier |
| Cost model | Free software; cost = infra + ops time (see burden) | Per-**observation** pricing — 9 units per pipeline run for us; free tier ~50k/mo | Per-**trace** + seats — 1 unit per run; free tier ~5k/mo. Same traffic, different billing dimension: our span-rich traces are 9× heavier on Langfuse's meter |
| Exit cost / lock-in | **~0** — data local, open source | **Low** — API export + self-host escape | **Moderate** — API export only, no self-host escape at our tier |
| Operational burden | **Highest** — storage (currently ephemeral: removing the container loses all traces), no auth by default, upgrades/backups ours | **None** on cloud (but vendor quirks are theirs to fix — see ingestion-version lag below); **high** if self-hosted (Postgres + ClickHouse + Redis + MinIO) | **None** |

Cross-cutting note on lock-in: instrumentation lock-in is zero by design (OTel/OTLP
contract — switching backends was a YAML edit). Exit cost grows only with adoption of
platform features beyond tracing (prompt management, managed evals, dashboards). While
usage stays tracing-only, all three remain near-freely interchangeable.

Operational findings from the comparison itself:
- Langfuse OTLP ingest needs the `x-langfuse-ingestion-version: 4` header, otherwise UI
  visibility lags the API by up to ~10 minutes.
- LangSmith is region-partitioned: EU accounts must use `eu.api.smith.langchain.com`
  (the US endpoint returns 403).
- The collector **drops** spans on permanent export errors (4xx) — per-backend failures
  are isolated, but data sent during a misconfiguration window is gone for that backend.

## Decision

No single winner — the criteria invert between candidates (Phoenix wins residency and
exit cost but carries all the ops burden; the clouds invert that). The decision is
therefore contextual:

- **Local development: Phoenix** (keep). Free, instant, data stays local, ephemerality
  is acceptable for dev.
- **If this becomes a team/production system: Langfuse** is the leading candidate —
  best trace UX, dual-dialect fidelity, EU cloud now with an open-source self-host
  escape hatch if data-residency requirements harden, and low exit cost.
- **LangSmith** is not eliminated but trails for this context: weakest residency story
  at our tier and no self-host escape; its strengths (polish, LangChain ecosystem,
  datasets/evals) sit largely outside this ADR's tracing-only scope.

The OTel Collector fan-out stays in place regardless — backends remain config, and
running more than one (e.g., Phoenix for dev + Langfuse for shared) costs one YAML block.

## Consequences

- The app keeps exporting to a single stable endpoint (`localhost:4318`); backend
  choices are revisited by editing `observability/otel-collector-config.yaml` only.
- Adopting any platform feature beyond tracing (prompt management, evals) re-opens the
  lock-in assessment — record it as a new ADR.
- Before any production use of Phoenix, its storage must be made persistent (volume +
  Postgres) and the UI put behind auth; alternatively rely on a cloud backend for shared
  visibility.
- The eval-platform comparison (out of scope here) may override the Langfuse-vs-LangSmith
  ranking, since eval capabilities differ more than tracing does.

## Follow-ups

- Evaluation/eval-platform comparison (LLM-as-judge, datasets, annotation queues) as a
  separate ADR — explicitly excluded from this decision.
- Tool-call spans and parallel agent execution would enrich the traces being compared.
