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

---

## 2026-07-10 — Day two: deriving ground truth and calibrating the judge

**What I built:** `evals/derive-ground-truth.ps1` (recomputes every expected answer from the mock data and shows its work — the answer key for the question set) and `evals/calibration-2026-07-10.md` (I blind-scored 10 traces, then compared against the judge).

**What I learned:**

1. **Ground truth should be derived, not hand-computed.** My hand-written expected answers were correct but unverifiable — checking one meant re-tracing mock JSON by hand. A script that computes them and shows the derivation made them checkable by reading. Bonus: it audited the questions themselves and found a quirk nobody wrote down (the most-expensive-order tie includes a cancelled order).

2. **Don't simplify questions to make them checkable — make the ground truth checkable instead.** My first instinct was to replace hard questions with easy-to-verify ones. That would optimize the eval for the evaluator's convenience: every question passes, nothing is learned.

3. **A planted defect must be able to change the answer, or it can't teach anything.** The cancelled-orders bug only affected a third-place tie, so no eval could ever catch it. We strengthened it: two cancelled Bluetooth Speaker orders now make the tool report a top seller of which 7 of 11 units were never sold. The app confidently recommended "doubling down" on it.

4. **The eval replay keeps finding non-eval bugs.** Today: a stale IDE classpath (the tracing bean is lazy — the app started healthy and died on the first request), and the discovery that yesterday's timeout fix never worked. The 180s config fed a bean that DI discarded (an `@Replaces` bean won, with a bare 60s-default model). Temperature 0.2 was never applied either.

5. **Verify the mechanism, not the lucky outcome.** Yesterday's clean 18/18 run "confirmed" the timeout fix — but a nondeterministic system can confirm a broken fix by luck. The model bean now logs its effective config at startup, so the next verification reads the mechanism directly.

6. **Calibration turned "I can't trust the judge" into a bias profile.** Blind-scoring 10 traces before looking at judge scores showed: agreement within ±0.15 on 7/10 (substantive reports, clean refusals); systematically **too generous** on detailed-but-factually-wrong answers (+0.42 — richness masquerades as quality); systematically **too harsh** on correct empty/"nothing found" answers (−0.38 — it punishes honesty for looking thin). Rule derived: low score on empty/refusal traces → human check before "fixing" anything; high score on a report certifies form only.

7. **Online scoring can't ask "is what the tools said true?"** I scored the trap answer 0.8 myself, because the app faithfully reported its tool's output — the judge and I were both blind to the business failure. Facts need offline evals against derived ground truth. Both of today's worst failures (catalog-wide reasons passed off as per-product, phantom best-seller) point there.

**End state:** 17/18 (Q11 hit the real 60s timeout — fix ready, activates on next restart). Judge calibrated with a written bias profile. Trap armed and demonstrated.

**Next:** offline evals — promote `questions.json` + derived ground truth to a dataset, let a reference-based check catch the phantom best-seller the online judge scored 0.88.
