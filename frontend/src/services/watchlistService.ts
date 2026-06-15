const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
  code: string | null;
};

export async function fetchWatchlist(): Promise<string[]> {
  const res = await fetch(`${API_URL}/api/v1/watchlist`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`watchlist fetch failed: ${res.status}`);
  const body = (await res.json()) as ApiResponse<string[]>;
  return body.success && body.data ? body.data : [];
}

export async function addWatchlist(symbol: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/v1/watchlist/${encodeURIComponent(symbol)}`, {
    method: "POST",
    credentials: "include",
  });
  if (!res.ok) throw new Error(`watchlist add failed: ${res.status}`);
}

export async function removeWatchlist(symbol: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/v1/watchlist/${encodeURIComponent(symbol)}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) throw new Error(`watchlist remove failed: ${res.status}`);
}
