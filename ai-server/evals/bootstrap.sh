#!/usr/bin/env bash
# 골든셋 15종목에 대해 뉴스 수집 + 임베딩. backend 를 통해 호출하므로 backend 가 실행 중이어야 한다.
#
# 인증: /api/v1/auth/dev-token 으로 JWT 즉시 발급 (tick.auth.dev-token-enabled=true 필요).
# 뉴스 소스 (네이버 / DART) 는 backend env 로 제어. 이 스크립트는 관여하지 않는다.
#
# Usage:
#   evals/bootstrap.sh                                      # localhost:8080 기본
#   evals/bootstrap.sh --api-base http://localhost:8080 --limit 20

set -euo pipefail

API_BASE="http://localhost:8080"
LIMIT=20

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api-base) API_BASE="$2"; shift 2;;
    --limit)    LIMIT="$2";    shift 2;;
    -h|--help)  head -n 10 "$0" | tail -n 9; exit 0;;
    *) echo "unknown arg: $1" >&2; exit 1;;
  esac
done

ROOT="$(cd "$(dirname "$0")" && pwd)"
GOLDEN="$ROOT/golden_set.jsonl"

echo "[bootstrap] api=$API_BASE limit=$LIMIT golden=$GOLDEN"

# 1) dev-token 발급 → Bearer JWT
echo "[bootstrap] issuing dev token"
TOKEN_JSON=$(curl -sf -X POST "$API_BASE/api/v1/auth/dev-token" -H "Content-Type: application/json" -d '{}') || {
  echo "[bootstrap] dev-token endpoint 실패. backend 가 tick.auth.dev-token-enabled=true 로 떠 있는지 확인." >&2
  exit 1
}
TOKEN=$(python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d['data']['accessToken'])" <<< "$TOKEN_JSON")
if [[ -z "$TOKEN" ]]; then
  echo "[bootstrap] token 파싱 실패. 응답: $TOKEN_JSON" >&2
  exit 1
fi
AUTH="Authorization: Bearer $TOKEN"
echo "[bootstrap] token acquired (…${TOKEN: -12})"

# 2) 골든셋 순회
SYMBOLS=$(python3 -c "import json; [print(json.loads(l)['symbol']) for l in open('$GOLDEN')]")
COUNT=$(wc -l <<< "$SYMBOLS" | tr -d ' ')
echo "[bootstrap] $COUNT symbols → collect + embed"

i=0
while IFS= read -r SYMBOL; do
  i=$((i+1))
  echo "  [$i/$COUNT] $SYMBOL"
  if OUT=$(curl -sf -X POST -H "$AUTH" \
    "$API_BASE/api/v1/news/$SYMBOL/collect?limit=$LIMIT" 2>&1); then
    FETCHED=$(python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d['data']['fetched'], d['data']['saved'])" <<< "$OUT" 2>/dev/null || echo "?")
    echo "    collect: fetched/saved = $FETCHED"
  else
    echo "    collect failed: $OUT"
  fi
  if OUT=$(curl -sf -X POST -H "$AUTH" \
    "$API_BASE/api/v1/ai/stocks/$SYMBOL/embed" 2>&1); then
    UPSERT=$(python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d['data']['upserted'])" <<< "$OUT" 2>/dev/null || echo "?")
    echo "    embed: upserted = $UPSERT"
  else
    echo "    embed failed: $OUT"
  fi
done <<< "$SYMBOLS"

echo "[bootstrap] done"
