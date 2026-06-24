import { ApiError, apiFetch, unwrapApi } from "./apiFetch";
import type { AiSummaryResponse } from "./aiSummaryClient";

// 타입은 client 용 모듈에서 정의/export — server 측에서도 동일하게 쓰도록 re-export.
export type { AiSummarySource, AiSummaryResponse } from "./aiSummaryClient";

/**
 * 종목 AI 요약을 backend (ai-server 프록시) 에서 가져온다 (**server component 전용**).
 *
 * `apiFetch` 가 `next/headers` (server-only API) 를 import 하므로, 이 함수도 client
 * 번들에 끌려가면 Next.js build 가 깨진다. client component 에서는
 * `aiSummaryClient.fetchAiSummaryFromBrowser` 를 쓸 것.
 *
 * 401 / 404 / 503 시 null. 그 외 에러는 throw.
 */
export async function fetchAiSummary(
  symbol: string,
): Promise<AiSummaryResponse | null> {
  const res = await apiFetch(`/api/v1/ai/stocks/${symbol}/summary`);
  try {
    return await unwrapApi<AiSummaryResponse>(res);
  } catch (err) {
    if (err instanceof ApiError && [401, 404, 503].includes(err.status)) {
      return null;
    }
    throw err;
  }
}
