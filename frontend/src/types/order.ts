export type OrderSide = "BUY" | "SELL";
export type OrderType = "MARKET" | "LIMIT";
export type OrderStatus = "PENDING" | "FILLED" | "CANCELED" | "REJECTED";

export type Order = {
  id: string;
  symbol: string;
  stockName: string;
  side: OrderSide;
  orderType: OrderType;
  quantity: number;
  price: number;
  filledQuantity?: number;
  status: OrderStatus;
  createdAt: string;
  filledAt?: string;
  averageCostAt?: number;
  realizedProfitLoss?: number;
};
