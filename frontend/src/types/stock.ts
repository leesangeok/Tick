export type Market = "KOSPI" | "KOSDAQ";

export type Stock = {
  symbol: string;
  name: string;
  market: Market;
  sector: string;
  currentPrice: number;
  changeAmount: number;
  changeRate: number;
  volume: number;
  isFavorite: boolean;
};

export type PricePoint = {
  timestamp: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};
