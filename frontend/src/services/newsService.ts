import { ApiError, apiFetch, unwrapApi } from "./apiFetch";

export type NewsResponse = {
  id: number;
  title: string;
  body: string;
  source: string | null;
  sourceUrl: string | null;
  publishedAt: string;
};

/**
 * 종목 관련 최근 뉴스. 401 / 404 시 빈 배열로 graceful.
 */
export async function fetchRecentNews(
  symbol: string,
  limit = 10,
): Promise<NewsResponse[]> {
  const res = await apiFetch(`/api/v1/news/${symbol}?limit=${limit}`);
  try {
    return await unwrapApi<NewsResponse[]>(res);
  } catch (err) {
    if (err instanceof ApiError && [401, 404].includes(err.status)) return [];
    throw err;
  }
}
