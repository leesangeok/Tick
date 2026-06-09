import type { Portfolio, Holding } from "@/types/portfolio";
import { mockAccount } from "./account";
import { mockStocks } from "./stocks";

const holdingSeeds = [
  { symbol: "005930", quantity: 50, averagePrice: 68000 },
  { symbol: "000660", quantity: 12, averagePrice: 180000 },
  { symbol: "373220", quantity: 5, averagePrice: 450000 },
  { symbol: "068270", quantity: 20, averagePrice: 175000 },
  { symbol: "005380", quantity: 8, averagePrice: 240000 },
];

export const mockHoldings: Holding[] = holdingSeeds.map((seed) => {
  const stock = mockStocks.find((s) => s.symbol === seed.symbol)!;
  const evaluationAmount = stock.currentPrice * seed.quantity;
  const costAmount = seed.averagePrice * seed.quantity;
  const profitLoss = evaluationAmount - costAmount;
  const profitRate = (profitLoss / costAmount) * 100;
  return {
    symbol: seed.symbol,
    name: stock.name,
    quantity: seed.quantity,
    averagePrice: seed.averagePrice,
    currentPrice: stock.currentPrice,
    evaluationAmount,
    profitLoss,
    profitRate,
  };
});

export function computePortfolio(
  cash: number,
  totalDeposits: number,
  realizedProfitLoss: number,
): Portfolio {
  const evaluationAmount = mockHoldings.reduce(
    (sum, h) => sum + h.evaluationAmount,
    0,
  );
  const holdingsCost = mockHoldings.reduce(
    (sum, h) => sum + h.averagePrice * h.quantity,
    0,
  );
  const unrealizedProfitLoss = evaluationAmount - holdingsCost;
  const unrealizedProfitRate = holdingsCost > 0
    ? (unrealizedProfitLoss / holdingsCost) * 100
    : 0;
  const totalProfitLoss = unrealizedProfitLoss + realizedProfitLoss;
  const totalProfitRate = totalDeposits > 0
    ? (totalProfitLoss / totalDeposits) * 100
    : 0;
  const todayProfitLoss = mockHoldings.reduce((sum, h) => {
    const stock = mockStocks.find((s) => s.symbol === h.symbol)!;
    return sum + h.quantity * stock.changeAmount;
  }, 0);
  const todayProfitRate = evaluationAmount > 0
    ? (todayProfitLoss / evaluationAmount) * 100
    : 0;

  return {
    cash,
    totalDeposits,
    totalAssets: cash + evaluationAmount,
    evaluationAmount,
    holdingsCost,
    unrealizedProfitLoss,
    unrealizedProfitRate,
    realizedProfitLoss,
    totalProfitLoss,
    totalProfitRate,
    todayProfitLoss,
    todayProfitRate,
    holdings: mockHoldings,
  };
}

export const mockPortfolio: Portfolio = computePortfolio(
  mockAccount.cash,
  mockAccount.totalDeposits,
  mockAccount.realizedProfitLoss,
);
