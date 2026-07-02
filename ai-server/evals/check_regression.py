"""Baseline / current eval 결과 JSON 비교 → 지표가 후퇴했으면 exit 1.

results/ 디렉터리에서 label hint 로 파일 매칭. baseline 은 라벨에 'baseline' 문자가 포함된
가장 오래된 결과, current 는 그 외 가장 최근 결과가 기본.

Thresholds (CLI override 가능):
- min_groundedness: current.groundedness_mean 이 이 값 이상
- max_hallucination_sum: current.hallucination_count_sum 이 이 값 이하
- groundedness_drop_tolerance: baseline 대비 하락 허용치 (개선을 강제하지는 않되 급락은 막음)

Usage:
    python -m evals.check_regression
    python -m evals.check_regression --baseline baseline-naver-only --current after-dart-added \\
        --min-groundedness 0.7 --max-hallucination-sum 5
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Any

RESULTS = Path(__file__).parent / "results"


def find_result(label_hint: str | None, prefer_oldest: bool) -> Path | None:
    candidates = sorted(RESULTS.glob("*.json"))
    if label_hint:
        candidates = [p for p in candidates if label_hint in p.stem]
    if not candidates:
        return None
    return candidates[0] if prefer_oldest else candidates[-1]


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--baseline", default="baseline", help="baseline 결과 label hint")
    p.add_argument("--current", default=None, help="현재 결과 label hint. 없으면 latest.")
    p.add_argument("--min-groundedness", type=float, default=0.65)
    p.add_argument("--max-hallucination-sum", type=int, default=15)
    p.add_argument("--groundedness-drop-tolerance", type=float, default=0.05)
    args = p.parse_args()

    baseline_path = find_result(args.baseline, prefer_oldest=True)
    if baseline_path is None:
        print("[eval-guard] no baseline result found; skipping (run evals/run_eval.py first)")
        return

    current_hint = args.current
    current_path = find_result(current_hint, prefer_oldest=False)
    # current 로 baseline 을 다시 잡지 않도록: hint 없으면 baseline 이 아닌 최신을 찾는다.
    if current_hint is None:
        others = sorted(RESULTS.glob("*.json"))
        others = [p for p in others if "baseline" not in p.stem]
        current_path = others[-1] if others else None

    if current_path is None or current_path == baseline_path:
        print(
            f"[eval-guard] only baseline found ({baseline_path.name}); "
            "no current run to compare — skipping regression check"
        )
        return

    baseline = load(baseline_path)
    current = load(current_path)
    b_agg = baseline.get("aggregate", {})
    c_agg = current.get("aggregate", {})

    print(f"[eval-guard] baseline = {baseline_path.name}")
    print(f"             aggregate = {json.dumps(b_agg, ensure_ascii=False)}")
    print(f"[eval-guard] current  = {current_path.name}")
    print(f"             aggregate = {json.dumps(c_agg, ensure_ascii=False)}")

    errors: list[str] = []
    b_ground = float(b_agg.get("groundedness_mean", 0.0))
    c_ground = float(c_agg.get("groundedness_mean", 0.0))
    if c_ground < args.min_groundedness:
        errors.append(f"groundedness_mean {c_ground:.3f} < min {args.min_groundedness}")
    if c_ground + args.groundedness_drop_tolerance < b_ground:
        errors.append(
            f"groundedness_mean drop {b_ground:.3f} → {c_ground:.3f} "
            f"exceeds tolerance {args.groundedness_drop_tolerance}"
        )
    c_halluc = int(c_agg.get("hallucination_count_sum", 0))
    b_halluc = int(b_agg.get("hallucination_count_sum", 0))
    if c_halluc > args.max_hallucination_sum:
        errors.append(f"hallucination_count_sum {c_halluc} > max {args.max_hallucination_sum}")
    if c_halluc > b_halluc:
        errors.append(f"hallucination_count_sum regressed {b_halluc} → {c_halluc}")

    if errors:
        print("[eval-guard] FAIL")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)
    print("[eval-guard] PASS")


if __name__ == "__main__":
    main()
