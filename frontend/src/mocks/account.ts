import type { Account, Transaction } from "@/types/account";

const transactions: Transaction[] = [
  {
    id: "tx_001",
    type: "DEPOSIT",
    amount: 25_000_000,
    createdAt: "2026-05-01T09:00:00+09:00",
  },
  {
    id: "tx_002",
    type: "SELL",
    symbol: "035720",
    stockName: "카카오",
    quantity: 30,
    price: 48_200,
    amount: 30 * 48_200,
    realizedProfitLoss: 171_000,
    createdAt: "2026-05-12T11:25:00+09:00",
  },
  {
    id: "tx_003",
    type: "SELL",
    symbol: "034020",
    stockName: "두산에너빌리티",
    quantity: 100,
    price: 21_000,
    amount: 100 * 21_000,
    realizedProfitLoss: 600_000,
    createdAt: "2026-05-21T14:08:00+09:00",
  },
  {
    id: "tx_004",
    type: "SELL",
    symbol: "012450",
    stockName: "한화에어로스페이스",
    quantity: 5,
    price: 285_000,
    amount: 5 * 285_000,
    realizedProfitLoss: 225_000,
    createdAt: "2026-05-28T10:42:00+09:00",
  },
  {
    id: "tx_005",
    type: "SELL",
    symbol: "005490",
    stockName: "POSCO홀딩스",
    quantity: 1,
    price: 380_000,
    amount: 380_000,
    realizedProfitLoss: 60_000,
    createdAt: "2026-06-03T13:15:00+09:00",
  },
  {
    id: "tx_006",
    type: "SELL",
    symbol: "068270",
    stockName: "셀트리온",
    quantity: 10,
    price: 188_400,
    amount: 10 * 188_400,
    realizedProfitLoss: 184_000,
    createdAt: "2026-06-05T15:02:00+09:00",
  },
];

const totalDeposits = transactions
  .filter((t) => t.type === "DEPOSIT")
  .reduce((sum, t) => sum + t.amount, 0);

const realizedProfitLoss = transactions
  .filter((t) => t.realizedProfitLoss !== undefined)
  .reduce((sum, t) => sum + (t.realizedProfitLoss ?? 0), 0);

const holdingsCost = 13_230_000;

export const mockAccount: Account = {
  totalDeposits,
  cash: totalDeposits + realizedProfitLoss - holdingsCost,
  realizedProfitLoss,
  transactions,
};
