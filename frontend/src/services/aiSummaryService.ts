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
 * 종목 AI 요약을 backend (ai-server 프록시) 에서 가져온다 (server component 용).
 *
 * - 401 (미인증) / 404 (요약 미생성 등) / 503 (ai-server 다운) 시 null 반환.
 * - 그 외 에러는 위로 throw.
 *
 * 주의: LLM 호출이 1.5~4s 걸려서 SSR 페이지를 차단함. 페이지 진입 속도가
 * 중요한 화면 (dashboard / ai-report / stocks/[symbol]) 에서는 client 컴포넌트
 * `LazyAiSummary` 로 페이지 렌더 후 lazy fetch 하도록 분리됨.
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

/**
 * 브라우저 (client component) 에서 직접 호출. 브라우저가 cookie 를 자동 전송하도록
 * `credentials: 'include'`. backend CORS 가 credentials + 도메인 허용 가정.
 *
 * 401/404/503/network 에러는 null 반환. 호출자는 null = "표시 안 함" 으로 처리.
 */
export async function fetchAiSummaryFromBrowser(
  symbol: string,
  apiBaseUrl: string,
): Promise<AiSummaryResponse | null> {
  try {
    const res = await fetch(`${apiBaseUrl}/api/v1/ai/stocks/${symbol}/summary`, {
      credentials: "include",
      cache: "no-store",
    });
    if (!res.ok) return null;
    const body = (await res.json()) as { success: boolean; data: unknown };
    return body?.success ? (body.data as AiSummaryResponse) : null;
  } catch {
    return null;
  }
}
