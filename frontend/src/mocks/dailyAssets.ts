import type { DailyAssetPoint } from "@/types/account";
import { mockAccount } from "./account";
import { mockPortfolio } from "./portfolio";

function mulberry32(seed: number) {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function generateDailyAssets(days = 30): DailyAssetPoint[] {
  const rand = mulberry32(20260608);
  const points: DailyAssetPoint[] = [];
  const end = mockPortfolio.totalAssets;
  const start = mockAccount.totalDeposits;
  const drift = (end - start) / days;
  const start0 = new Date();
  start0.setHours(0, 0, 0, 0);

  let value = start;
  for (let i = 0; i < days; i++) {
    const date = new Date(start0);
    date.setDate(date.getDate() - (days - 1 - i));
    const noise = (rand() - 0.5) * 600_000;
    value = value + drift + noise;
    points.push({
      date: date.toISOString().split("T")[0],
      totalAssets: Math.round(value),
      profitLoss: Math.round(value - start),
    });
  }
  points[points.length - 1]!.totalAssets = end;
  points[points.length - 1]!.profitLoss = end - start;
  return points;
}

export const mockDailyAssets = generateDailyAssets(30);
