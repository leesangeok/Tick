import { fetchOrders } from "@/services/orderService";
import {
  formatCurrency,
  formatRelativeTime,
  formatSignedCurrency,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import type { OrderStatus } from "@/types/order";

const statusLabel: Record<OrderStatus, string> = {
  PENDING: "대기",
  FILLED: "체결",
  CANCELED: "취소",
  REJECTED: "거절",
};

const statusClass: Record<OrderStatus, string> = {
  PENDING: "bg-muted text-muted-foreground",
  FILLED: "bg-gain/15 text-gain",
  CANCELED: "bg-muted text-muted-foreground",
  REJECTED: "bg-loss/15 text-loss",
};

export default async function OrdersPage() {
  const orders = await fetchOrders();
  const totalRealized = orders.reduce(
    (sum, o) => sum + (o.realizedProfitLoss ?? 0),
    0,
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">주문 내역</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          매수·매도 주문과 체결 내역, 실현 손익을 확인하세요.
        </p>
      </div>

      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">누적 실현 손익</p>
        <p
          className={cn(
            "mt-1 text-xl font-bold tabular-nums",
            priceDirectionClass(totalRealized),
          )}
        >
          {formatSignedCurrency(totalRealized)}
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {["전체", "매수", "매도", "대기", "체결", "취소"].map((tag, i) => (
          <button
            key={tag}
            type="button"
            className={cn(
              "rounded-full border px-3 py-1.5 text-xs font-medium",
              i === 0
                ? "border-foreground bg-foreground text-background"
                : "border-border text-muted-foreground hover:bg-accent hover:text-foreground",
            )}
          >
            {tag}
          </button>
        ))}
      </div>

      <div className="overflow-x-auto rounded-lg border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">시각</th>
              <th className="px-4 py-3 text-left font-medium">종목</th>
              <th className="px-4 py-3 text-center font-medium">구분</th>
              <th className="px-4 py-3 text-right font-medium">수량</th>
              <th className="px-4 py-3 text-right font-medium">주문 가격</th>
              <th className="px-4 py-3 text-right font-medium">실현 손익</th>
              <th className="px-4 py-3 text-center font-medium">상태</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {orders.map((o) => (
              <tr key={o.id} className="hover:bg-accent/40">
                <td className="px-4 py-3 text-xs text-muted-foreground">
                  {formatRelativeTime(o.createdAt)}
                </td>
                <td className="px-4 py-3">
                  <p className="font-medium">{o.stockName}</p>
                  <p className="text-xs text-muted-foreground">{o.symbol}</p>
                </td>
                <td className="px-4 py-3 text-center">
                  <span
                    className={cn(
                      "rounded px-2 py-0.5 text-xs font-medium",
                      o.side === "BUY"
                        ? "bg-gain/15 text-gain"
                        : "bg-loss/15 text-loss",
                    )}
                  >
                    {o.side === "BUY" ? "매수" : "매도"}{" "}
                    {o.orderType === "MARKET" ? "(시장)" : "(지정)"}
                  </span>
                </td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {o.quantity}주
                </td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {formatCurrency(o.price)}
                </td>
                <td
                  className={cn(
                    "px-4 py-3 text-right tabular-nums",
                    o.realizedProfitLoss != null
                      ? priceDirectionClass(o.realizedProfitLoss)
                      : "text-muted-foreground/40",
                  )}
                >
                  {o.realizedProfitLoss != null
                    ? formatSignedCurrency(o.realizedProfitLoss)
                    : "—"}
                </td>
                <td className="px-4 py-3 text-center">
                  <span
                    className={cn(
                      "rounded-full px-2 py-0.5 text-xs",
                      statusClass[o.status],
                    )}
                  >
                    {statusLabel[o.status]}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
