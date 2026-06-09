import type { PricePoint, Stock } from "@/types/stock";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function fetchStocks(): Promise<Stock[]> {
  const res = await fetch(`${API_URL}/api/stocks`, { cache: "no-store" });
  if (!res.ok) {
    throw new Error(`Failed to fetch stocks: ${res.status}`);
  }
  return res.json();
}

export async function fetchStock(symbol: string): Promise<Stock | null> {
  const res = await fetch(`${API_URL}/api/stocks/${symbol}`, {
    cache: "no-store",
  });
  if (res.status === 404) return null;
  if (!res.ok) {
    throw new Error(`Failed to fetch stock ${symbol}: ${res.status}`);
  }
  return res.json();
}

export async function fetchPriceSeries(
  symbol: string,
  days = 60,
): Promise<PricePoint[]> {
  const res = await fetch(
    `${API_URL}/api/stocks/${symbol}/prices?days=${days}`,
    { cache: "no-store" },
  );
  if (!res.ok) {
    throw new Error(`Failed to fetch prices for ${symbol}: ${res.status}`);
  }
  return res.json();
}
