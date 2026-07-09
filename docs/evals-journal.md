# Evals journal

Short field notes from the evals learning journey on shopMultiAgent.

---

## 2026-07-09 — Day one: online evals with an LLM judge (Langfuse)

**What I built:** 18 test questions with hand-computed ground truth (`evals/questions.json`), a script that sends them to the app (`evals/replay.ps1`), and an LLM-as-judge evaluator in Langfuse that scores every incoming trace 0–1 on relevance, specificity, honesty, and usability.

**What I learned:**

1. **Writing eval questions revealed what my app actually is.** I thought it was a support bot; the tools say it's an analytics assistant. You can't write good test questions without understanding your system precisely.

2. **The questions found two real bugs before any judge scored anything.** A polite refusal ("I can't answer weather questions") crashed the app, because the researcher's structured output contract had no legal way to say "I can't answer". And long reports died on a 60s timeout. Fixed both: added `unableToAnswer` fields to the contract, raised the timeout.

3. **Failures are nondeterministic.** The same question crashed in one run and passed the next. One passing test proves nothing about an LLM app — evals measure a failure *rate*, which is why you need many questions, not five.

4. **The eval audited my instrumentation for free.** The traces had no input/output on the root span, so the judge had nothing to read. Evals force your observability to be correct.

5. **The judge is a smoke detector, not an auditor.** It correctly flagged the writer as overconfident (big recommendations from 8 data points, no caveats) — but it also praised wrong numbers as "specific" because it has no ground truth. LLM judges catch problems of *form*; catching problems of *fact* needs offline evals with expected answers.

6. **Langfuse UX traps:** the setup wizard creates a *new* evaluator (doesn't edit the old one), "Execute" means save-and-activate, and the default variable mapping was wrong (both variables on Input → judge scored 0). Debug tool: the judge call is itself a trace — read it to see what the judge actually received.

7. **Every fix needs a regression check.** The refusal escape hatch could have made the model lazily refuse hard questions. One eval question (ORD-021, answerable only indirectly) existed exactly to catch that — it passed.

**End state:** 18/18 questions answered, zero crashes, all scored in Langfuse.

**Next:** writer-conciseness experiment (prompt change, compare scores before/after), code-based evaluators, offline datasets, same eval in LangSmith + Phoenix. Still unsurfaced by evals: the top-sellers tool counts cancelled orders.
