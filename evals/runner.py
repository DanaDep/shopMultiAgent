"""Offline eval runner: dataset.json -> app -> scorers -> Phoenix experiment.

Steps:
  1. Load evals/dataset.json (questions + ground truth + categories).
  2. Upload it as a Phoenix dataset (skipped if it already exists).
  3. Run every question through the app (POST /api/chat) as a Phoenix experiment.
  4. Score each answer with code evaluators; results land in the Phoenix UI,
     where experiments on the same dataset can be compared side by side.

Prerequisites:
  pip install arize-phoenix requests "anthropic[bedrock]"
  docker compose -f observability/docker-compose.yml up -d   # Phoenix server
  ./gradlew run                                              # the app
  valid AWS credentials (for --judge)

Usage:
  python evals/runner.py                          # full run, code scorers only
  python evals/runner.py --smoke 2                # call app with 2 questions, no Phoenix
  python evals/runner.py --experiment-name baseline --judge   # full run incl. LLM judge
  python evals/runner.py --rescore <EXPERIMENT_ID> --judge    # judge existing answers, no app calls
"""

import argparse
import json
import re
import sys
from pathlib import Path

import requests

# The Phoenix SDK prints emoji; Windows consoles default to cp1252 and crash on them.
sys.stdout.reconfigure(encoding="utf-8")

DATASET_FILE = Path(__file__).parent / "dataset.json"
MONEY_RE = re.compile(r"\$[\d,]+\.\d{2}")


def load_dataset():
    data = json.loads(DATASET_FILE.read_text(encoding="utf-8"))
    return data["questions"]


def ask_app(base_url: str, question: str) -> str:
    # The endpoint expects a JSON-encoded string, same as the UI sends.
    resp = requests.post(f"{base_url}/api/chat", json=question, timeout=200)
    resp.raise_for_status()
    return resp.text


# --- evaluators -------------------------------------------------------------
# Phoenix binds evaluator arguments by name: `output` is the task's return
# value, `expected` is the example's output dict (our groundTruth), `metadata`
# carries id/category/watchFor.

def key_numbers_present(output, expected):
    """Fraction of dollar amounts from the ground truth that appear in the answer.

    Skips (score=None) questions whose ground truth contains no dollar
    amounts — those need the reference-aware judge, not this check.
    """
    amounts = set(MONEY_RE.findall(expected["groundTruth"]))
    if not amounts:
        # (score, label, explanation) with score=None records a skip, not a fake pass
        return (None, "n/a", "no dollar amounts in ground truth; needs the reference judge")
    hits = [a for a in amounts if a in output]
    score = len(hits) / len(amounts)
    return (score, "pass" if score == 1.0 else "fail",
            f"expected {sorted(amounts)}, found {sorted(hits)}")


def answered_when_empty_expected(output, expected, metadata):
    """For temporal-empty questions the correct answer is 'nothing/none'.

    Scores 1.0 if the answer contains a none/zero signal, 0.0 otherwise —
    a cheap hallucination tripwire. Other categories are skipped (score=None).
    """
    if metadata.get("category") != "temporal-empty":
        return (None, "n/a", "only applies to temporal-empty questions")
    signals = ("no ", "none", "zero", "0 ", "not find", "no orders", "no refunds")
    honest = any(s in output.lower() for s in signals)
    return (1.0 if honest else 0.0, "pass" if honest else "fail",
            "answer acknowledges emptiness" if honest else "expected 'nothing/none' but answer reports data")


def make_reference_judge(model: str, aws_region: str):
    """Reference-aware LLM judge: grades factual agreement with the ground truth.

    Unlike the online (reference-free) judge, this one is given the answer key
    and the known failure modes, so a high score certifies facts, not form.
    Returns a categorical verdict mapped to 1.0 / 0.5 / 0.0 — judges are more
    reliable on categories than on continuous scales.
    """
    from anthropic import AnthropicBedrock

    client = AnthropicBedrock(aws_region=aws_region)

    def reference_judge(input, output, expected, metadata):
        prompt = f"""You are grading an AI shopping-analytics assistant's answer against a known correct answer.

Question: {input["question"]}
Category: {metadata.get("category", "")}
Correct answer (ground truth): {expected["groundTruth"]}
Known failure modes to watch for: {metadata.get("watchFor", "none listed")}

Assistant's answer:
{output}

Grade ONLY factual agreement with the ground truth — not style, length, or detail:
- "correct": every fact in the answer agrees with the ground truth and nothing is invented. Short, honest "none/nothing found" answers are correct when the ground truth says so. A graceful refusal is correct when the ground truth calls for one.
- "partial": the main conclusion matches but details are wrong or missing, ties are misrepresented as definite orders, or the answer refuses where the ground truth shows it was answerable.
- "incorrect": the main conclusion contradicts the ground truth, or the answer states facts not supported by it.

Respond with ONLY a JSON object: {{"verdict": "correct" | "partial" | "incorrect", "reason": "<one sentence>"}}"""

        resp = client.messages.create(
            model=model,
            max_tokens=300,
            messages=[{"role": "user", "content": prompt}],
        )
        text = "".join(b.text for b in resp.content if b.type == "text")
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if not match:
            return (None, "judge-error", f"unparseable judge output: {text[:120]}")
        verdict = json.loads(match.group(0))
        scores = {"correct": 1.0, "partial": 0.5, "incorrect": 0.0}
        label = verdict.get("verdict", "")
        if label not in scores:
            return (None, "judge-error", f"unknown verdict: {label}")
        return (scores[label], label, verdict.get("reason", ""))

    return reference_judge


# --- main -------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Run offline evals against Phoenix.")
    parser.add_argument("--base-url", default="http://localhost:9090")
    parser.add_argument("--phoenix", default="http://localhost:6006")
    parser.add_argument("--dataset-name", default="shop-eval")
    parser.add_argument("--experiment-name", default=None,
                        help="name shown in the Phoenix experiments list, e.g. 'baseline' or 'fix-cancelled-orders'")
    parser.add_argument("--smoke", type=int, metavar="N",
                        help="send only the first N questions to the app and print answers; no Phoenix involved")
    parser.add_argument("--judge", action="store_true",
                        help="also score with the reference-aware LLM judge (needs valid AWS credentials)")
    parser.add_argument("--judge-model", default="eu.anthropic.claude-sonnet-4-6",
                        help="Bedrock model ID for the judge")
    parser.add_argument("--aws-region", default="eu-west-1")
    parser.add_argument("--rescore", metavar="EXPERIMENT_ID",
                        help="re-run evaluators on an existing experiment's stored answers; no app calls")
    args = parser.parse_args()

    questions = load_dataset()

    if args.smoke:
        for q in questions[: args.smoke]:
            print(f"[{q['id']}] ({q['category']}) {q['question']}")
            answer = ask_app(args.base_url, q["question"])
            print(f"  -> {answer[:200]}\n")
        return

    from phoenix.client import Client
    from phoenix.client.experiments import run_experiment

    client = Client(base_url=args.phoenix)

    evaluators = [key_numbers_present, answered_when_empty_expected]
    if args.judge:
        evaluators.append(make_reference_judge(args.judge_model, args.aws_region))

    if args.rescore:
        from phoenix.client.experiments import evaluate_experiment, get_experiment

        experiment = get_experiment(experiment_id=args.rescore, client=client)
        evaluate_experiment(experiment=experiment, evaluators=evaluators, client=client)
        print(f"Re-scored experiment {args.rescore}. Compare runs at {args.phoenix}/datasets")
        return

    try:
        dataset = client.datasets.get_dataset(dataset=args.dataset_name)
        print(f"Using existing Phoenix dataset '{args.dataset_name}'")
    except Exception:
        dataset = client.datasets.create_dataset(
            name=args.dataset_name,
            inputs=[{"question": q["question"]} for q in questions],
            outputs=[{"groundTruth": q["groundTruth"]} for q in questions],
            metadata=[{"id": q["id"], "category": q["category"], "watchFor": q.get("watchFor", "")}
                      for q in questions],
        )
        print(f"Created Phoenix dataset '{args.dataset_name}' ({len(questions)} examples)")

    def task(input):
        return ask_app(args.base_url, input["question"])

    run_experiment(
        dataset=dataset,
        task=task,
        evaluators=evaluators,
        experiment_name=args.experiment_name,
        experiment_description="offline eval run via evals/runner.py",
        timeout=300,  # per-question; the app itself may take up to 180s
    )
    print(f"Done. Compare runs at {args.phoenix}/datasets")


if __name__ == "__main__":
    sys.exit(main())
