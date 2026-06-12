import { fetchOrders } from "@/services/orderService";
import { OrdersTable } from "@/components/orders/OrdersTable";
import { formatSignedCurrency, priceDirectionClass } from "@/lib/format";
import { cn } from "@/lib/utils";

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

      <OrdersTable orders={orders} />
    </div>
  );
}
