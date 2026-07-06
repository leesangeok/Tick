#!/usr/bin/env bash
# 한 커맨드로 bootstrap (뉴스 수집 + 임베딩) + eval 러너 실행.
#
# 흐름:
#   1. baseline 실행: backend 를 DART=false 상태로 기동 후 이 스크립트 실행
#      $ evals/harness.sh baseline-naver-only
#   2. after 실행: backend 를 DART=true 로 재기동 후 다시 실행
#      $ evals/harness.sh after-dart-added
#
# 필요 env (이 프로세스 안):
#   OPENAI_API_KEY, ANTHROPIC_API_KEY (LLM 호출)
#   POSTGRES_DSN (백엔드와 같은 DB. 기본 postgresql://tick:tick@localhost:5432/tick)

set -euo pipefail

LABEL="${1:-}"
if [[ -z "$LABEL" ]]; then
  cat <<EOF
usage: $0 <label> [bootstrap options]
example: $0 baseline-naver-only
         $0 after-dart-added --limit 30
EOF
  exit 1
fi
shift || true

ROOT="$(cd "$(dirname "$0")" && pwd)"

"$ROOT/bootstrap.sh" "$@"

echo "[harness] running eval label=$LABEL"
cd "$ROOT/.."

if command -v uv >/dev/null 2>&1; then
  uv run python -m evals.run_eval --label "$LABEL"
else
  python3 -m evals.run_eval --label "$LABEL"
fi
