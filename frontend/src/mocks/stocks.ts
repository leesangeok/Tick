import type { Stock } from "@/types/stock";
import { getPriceSeries } from "./priceSeries";

type StockSeed = {
  symbol: string;
  name: string;
  market: Stock["market"];
  sector: string;
  basePrice: number;
  isFavorite?: boolean;
};

const seeds: StockSeed[] = [
  { symbol: "005930", name: "삼성전자", market: "KOSPI", sector: "반도체", basePrice: 71500, isFavorite: true },
  { symbol: "000660", name: "SK하이닉스", market: "KOSPI", sector: "반도체", basePrice: 195000, isFavorite: true },
  { symbol: "035420", name: "NAVER", market: "KOSPI", sector: "인터넷", basePrice: 218000 },
  { symbol: "035720", name: "카카오", market: "KOSPI", sector: "인터넷", basePrice: 45200 },
  { symbol: "005380", name: "현대차", market: "KOSPI", sector: "자동차", basePrice: 248500 },
  { symbol: "373220", name: "LG에너지솔루션", market: "KOSPI", sector: "2차전지", basePrice: 412000, isFavorite: true },
  { symbol: "068270", name: "셀트리온", market: "KOSPI", sector: "바이오", basePrice: 192800 },
  { symbol: "012450", name: "한화에어로스페이스", market: "KOSPI", sector: "방산", basePrice: 287500 },
  { symbol: "034020", name: "두산에너빌리티", market: "KOSPI", sector: "기계", basePrice: 19840 },
  { symbol: "005490", name: "POSCO홀딩스", market: "KOSPI", sector: "철강", basePrice: 358000 },
];

export const mockStocks: Stock[] = seeds.map((seed) => {
  const series = getPriceSeries(seed.symbol, seed.basePrice, 60);
  const last = series[series.length - 1]!;
  const prev = series[series.length - 2]!;
  const changeAmount = last.close - prev.close;
  const changeRate = (changeAmount / prev.close) * 100;
  return {
    symbol: seed.symbol,
    name: seed.name,
    market: seed.market,
    sector: seed.sector,
    currentPrice: last.close,
    changeAmount,
    changeRate,
    volume: last.volume,
    isFavorite: seed.isFavorite ?? false,
  };
});

export function getStockBySymbol(symbol: string): Stock | undefined {
  return mockStocks.find((s) => s.symbol === symbol);
}

export function getStockBasePrice(symbol: string): number {
  return seeds.find((s) => s.symbol === symbol)?.basePrice ?? 10000;
}
