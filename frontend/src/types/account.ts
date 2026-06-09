export type TransactionType = "DEPOSIT" | "WITHDRAW" | "BUY" | "SELL";

export type Transaction = {
  id: string;
  type: TransactionType;
  amount: number;
  symbol?: string | null;
  stockName?: string | null;
  quantity?: number | null;
  price?: number | null;
  realizedProfitLoss?: number | null;
  createdAt: string;
};

export type Account = {
  totalDeposits: number;
  cash: number;
  realizedProfitLoss: number;
  transactions: Transaction[];
};

export type DailyAssetPoint = {
  date: string;
  totalAssets: number;
  profitLoss: number;
};
