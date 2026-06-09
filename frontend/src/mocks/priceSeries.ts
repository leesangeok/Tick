import type { PricePoint } from "@/types/stock";

function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i);
    hash |= 0;
  }
  return hash;
}

function mulberry32(seed: number) {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function generatePriceSeries(
  symbol: string,
  basePrice: number,
  days = 60,
): PricePoint[] {
  const rand = mulberry32(hashString(symbol));
  const points: PricePoint[] = [];
  let price = basePrice;
  const start = new Date();
  start.setHours(0, 0, 0, 0);

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(start);
    date.setDate(date.getDate() - i);

    const drift = (rand() - 0.48) * 0.05;
    const open = price;
    const close = Math.max(price * (1 + drift), basePrice * 0.5);
    const high = Math.max(open, close) * (1 + rand() * 0.012);
    const low = Math.min(open, close) * (1 - rand() * 0.012);
    const volume = Math.floor(rand() * 5_000_000 + 800_000);

    points.push({
      timestamp: date.toISOString().split("T")[0],
      open: Math.round(open),
      high: Math.round(high),
      low: Math.round(low),
      close: Math.round(close),
      volume,
    });

    price = close;
  }

  return points;
}

const cache = new Map<string, PricePoint[]>();

export function getPriceSeries(
  symbol: string,
  basePrice: number,
  days = 60,
): PricePoint[] {
  const key = `${symbol}:${basePrice}:${days}`;
  let series = cache.get(key);
  if (!series) {
    series = generatePriceSeries(symbol, basePrice, days);
    cache.set(key, series);
  }
  return series;
}
