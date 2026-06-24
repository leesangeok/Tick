/**
 * 브라우저 (client component) 에서 직접 호출하는 AI summary fetcher.
 *
 * **이 파일은 절대 `apiFetch` 등 server-only API 를 import 하지 않는다.**
 * `apiFetch` 가 `next/headers` 의 `cookies()` 를 사용하기 때문에, client 번들에
 * 끌려 들어가면 Next.js build 가 깨진다. 그래서 client 전용 함수와 타입을
 * server 용 `aiSummaryService.ts` 와 물리적으로 분리.
 *
 * client 컴포넌트 (`LazyAiSummary` 등) 는 이 파일만 import.
 */

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
 * 브라우저가 same-site cookie (tick_at) 를 자동 전송하도록 `credentials: 'include'`.
 * backend CORS 가 credentials + 도메인 허용 가정 (prod 는 .tickk.dev sub-domain 공유).
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
