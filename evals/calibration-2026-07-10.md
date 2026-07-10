# Judge calibration — 2026-07-10

Goal: measure where the LLM judge (`answer-quality` in Langfuse) can be trusted and
where it is blind, by comparing its scores against my own on the same traces.

## Protocol (order matters)

1. Open the trace in Langfuse. Read ONLY the question (input) and the answer (output).
   Do NOT look at the Scores section yet.
2. Decide my score 0.0–1.0 with the answer key open (`derive-ground-truth.ps1` output).
   Question to answer: "Would I accept this as a correct, honest, useful reply?"
3. Write my score in the table below FIRST. Only then reveal the judge's score.
4. Notes: one line on WHY we differ, when we differ.

## Scoring sheet

Suggested mix: easy facts, the trap, empty-result, indirect edge, refusals.

| Q   | Category       | My score (FIRST) | Judge score | Gap (judge − me) | Why we differ |
|-----|----------------|------------------|-------------|------------------|---------------|
| Q01 | single-tool    | 0.4              | 0.82        | **+0.42**        | Answer presented catalog-wide return reasons as if they were the Winter Jacket's (its real reasons: 2x wrong size, 1x damaged) and built recommendations on that. Wrong facts, rich detail — judge rewarded the detail, blind to the facts. |
| Q03 | single-tool    | 0.7              | 0.85        | +0.15            | Top 1 and 2 swapped — both are 5.0 ties, but the answer claimed a definite order instead of acknowledging the tie. |
| Q06 | single-tool    | 0.9              | 0.82        | −0.08            | Agree. |
| Q07 | single-tool (TRAP) | 0.8          | 0.88        | +0.08            | Scored against the TOOL answer (Bluetooth 11), so judge and I agree; the same-unit tie was swapped. The business-truth failure (7 of 11 units cancelled) is invisible to BOTH online scorers — this is the offline-evals gap, not a judge-vs-me gap. |
| Q09 | multi-tool     | 0.9              | 0.88        | −0.02            | Agree. |
| Q10 | multi-tool     | 0.9              | 0.93        | +0.03            | Agree. |
| Q13 | temporal-empty | 1.0              | 0.62        | **−0.38**        | "Zero orders" is the CORRECT answer, delivered honestly. Judge reads a short, data-empty answer as low quality. |
| Q15 | edge-case      | 1.0              | 0.9         | −0.10            | Agree. |
| Q16 | edge-case      | 0.8              | 0.6         | −0.20            | App said "can't look up by ID" instead of "no such order" (didn't try the year listing) — a real flaw, but the judge punished the honest refusal harder than I did. |
| Q18 | out-of-scope   | 1.0              | 0.9         | −0.10            | Agree. |

## Calibration statement

- **Judge agrees with me within ±0.15 on:** substantive reports and clean refusals — Q03, Q06, Q07, Q09, Q10, Q15, Q18 (7 of 10). In this zone the judge is a usable proxy for my judgment.
- **Judge is systematically too generous on:** detailed-but-factually-wrong reports (Q01, +0.42). It has no ground truth, so richness of detail masquerades as quality. A high judge score says NOTHING about factual correctness.
- **Judge is systematically too harsh on:** correct empty/negative answers (Q13 −0.38, Q16 −0.20). It penalizes short honest "nothing found" answers exactly when honesty is the achievement.
- **Traces where the judge must NOT be trusted alone:** (1) any data-heavy report where facts could be wrong — high score ≠ correct; (2) temporal-empty and refusal traces — low score ≠ bad answer.

**Operational rule derived:** a LOW judge score on an empty/refusal trace goes to a human before anyone "fixes" the app for it; a HIGH judge score on a report certifies form only — factual checks need offline evals with ground truth.
