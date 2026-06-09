export type Holding = {
  symbol: string;
  name: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  evaluationAmount: number;
  profitLoss: number;
  profitRate: number;
};

export type Portfolio = {
  cash: number;
  totalDeposits: number;
  totalAssets: number;
  evaluationAmount: number;
  holdingsCost: number;
  unrealizedProfitLoss: number;
  unrealizedProfitRate: number;
  realizedProfitLoss: number;
  totalProfitLoss: number;
  totalProfitRate: number;
  todayProfitLoss: number;
  todayProfitRate: number;
  holdings: Holding[];
};
