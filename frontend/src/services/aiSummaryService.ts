import { ApiError, apiFetch, unwrapApi } from "./apiFetch";

export type AiSummarySource = {
  title: string;
  source: string | null;
  sourceUrl: string | null;
  publishedAt: string;
};

export type AiSummaryResponse = {
  symbol: string;
  summary: string;
  keyReasons: string[];
  riskNotes: string[];
  sources: AiSummarySource[];
  retrievedCount: number;
};

/**
 * 종목 AI 요약을 backend (ai-server 프록시) 에서 가져온다.
 *
 * - 401 (미인증) / 404 (요약 미생성 등) / 503 (ai-server 다운) 시 null 반환.
 * - 그 외 에러는 위로 throw.
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
