import type { PricePoint, Stock } from "@/types/stock";
import { ApiError, apiFetch, unwrapApi } from "./apiFetch";

export async function fetchStocks(): Promise<Stock[]> {
  const res = await apiFetch("/api/v1/stocks");
  return unwrapApi<Stock[]>(res);
}

export async function fetchStock(symbol: string): Promise<Stock | null> {
  const res = await apiFetch(`/api/v1/stocks/${symbol}`);
  try {
    return await unwrapApi<Stock>(res);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) return null;
    throw err;
  }
}

export async function fetchPriceSeries(
  symbol: string,
  days = 60,
): Promise<PricePoint[]> {
  const res = await apiFetch(`/api/v1/stocks/${symbol}/prices?days=${days}`);
  return unwrapApi<PricePoint[]>(res);
}
