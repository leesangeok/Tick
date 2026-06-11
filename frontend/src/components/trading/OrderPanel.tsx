"use client";

import { useMemo, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  createBuyOrder,
  createSellOrder,
  OrderApiError,
} from "@/services/orderClient";
import {
  formatCurrency,
  formatKoreanUnit,
  formatSignedCurrency,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import type { OrderType } from "@/types/order";

type Props = {
  stockCode: string;
  stockName: string;
  currentPrice: number;
  availableCash: number;
};

type Side = "BUY" | "SELL";

export function OrderPanel({
  stockCode,
  stockName,
  currentPrice,
  availableCash,
}: Props) {
  const router = useRouter();
  const [side, setSide] = useState<Side>("BUY");
  const [orderType, setOrderType] = useState<OrderType>("MARKET");
  const [quantity, setQuantity] = useState<string>("");
  const [confirming, setConfirming] = useState(false);
  const [pending, startTransition] = useTransition();

  const qtyNumber = Number(quantity) || 0;
  const estimatedAmount = useMemo(
    () => Math.max(0, qtyNumber) * currentPrice,
    [qtyNumber, currentPrice],
  );
  const canSubmit = qtyNumber > 0 && !pending;

  function reset() {
    setQuantity("");
    setConfirming(false);
  }

  function handleClickSubmit() {
    if (!canSubmit) return;
    setConfirming(true);
  }

  function confirmOrder() {
    if (!canSubmit) return;
    startTransition(async () => {
      try {
        const fn = side === "BUY" ? createBuyOrder : createSellOrder;
        const result = await fn({
          stockCode,
          quantity: qtyNumber,
          orderType,
        });
        const filledPriceLabel = formatCurrency(result.price);
        const realizedLabel =
          result.side === "SELL" && result.realizedProfitLoss != null
            ? ` · 실현손익 ${formatSignedCurrency(result.realizedProfitLoss)}`
            : "";
        toast.success(
          `${stockName} ${result.side === "BUY" ? "매수" : "매도"} 체결`,
          {
            description: `${result.quantity}주 @ ${filledPriceLabel}${realizedLabel}`,
          },
        );
        reset();
        router.refresh();
      } catch (err) {
        const message =
          err instanceof OrderApiError
            ? err.message
            : err instanceof Error
              ? err.message
              : "주문 처리 중 오류가 발생했습니다.";
        toast.error("주문 실패", { description: message });
        setConfirming(false);
      }
    });
  }

  const submitColor =
    side === "BUY"
      ? "bg-gain text-white hover:bg-gain/90"
      : "bg-loss text-white hover:bg-loss/90";

  return (
    <div className="rounded-lg border border-border bg-card">
      <div className="grid grid-cols-2 border-b border-border">
        <button
          type="button"
          onClick={() => {
            setSide("BUY");
            setConfirming(false);
          }}
          className={cn(
            "border-r border-border px-4 py-3 text-sm font-semibold transition",
            side === "BUY"
              ? "bg-gain/10 text-gain"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          매수
        </button>
        <button
          type="button"
          onClick={() => {
            setSide("SELL");
            setConfirming(false);
          }}
          className={cn(
            "px-4 py-3 text-sm font-semibold transition",
            side === "SELL"
              ? "bg-loss/10 text-loss"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          매도
        </button>
      </div>

      <div className="space-y-4 p-4">
        <div className="flex gap-2 text-xs">
          {(["MARKET", "LIMIT"] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setOrderType(t)}
              className={cn(
                "flex-1 rounded border py-1.5 font-medium transition",
                orderType === t
                  ? "border-foreground bg-foreground text-background"
                  : "border-border text-muted-foreground hover:text-foreground",
              )}
            >
              {t === "MARKET" ? "시장가" : "지정가"}
            </button>
          ))}
        </div>

        <div>
          <label className="text-xs text-muted-foreground" htmlFor="order-quantity">
            수량
          </label>
          <input
            id="order-quantity"
            type="number"
            min={1}
            inputMode="numeric"
            placeholder="0"
            value={quantity}
            onChange={(e) => {
              setQuantity(e.target.value);
              setConfirming(false);
            }}
            disabled={pending}
            className="mt-1 w-full rounded border border-border bg-background px-3 py-2 text-right text-sm tabular-nums outline-none focus:border-foreground"
          />
        </div>

        <div>
          <label className="text-xs text-muted-foreground" htmlFor="order-price">
            현재가 (체결 기준)
          </label>
          <input
            id="order-price"
            type="number"
            value={currentPrice}
            readOnly
            disabled
            className="mt-1 w-full rounded border border-border bg-background px-3 py-2 text-right text-sm tabular-nums text-muted-foreground"
          />
          <p className="mt-1 text-[10px] text-muted-foreground">
            모의 환경에서는 현재가로 즉시 체결됩니다.
          </p>
        </div>

        <div className="rounded bg-muted/50 p-3 text-xs">
          <div className="flex justify-between">
            <span className="text-muted-foreground">예상 주문 금액</span>
            <span className="tabular-nums">{formatCurrency(estimatedAmount)}</span>
          </div>
          <div className="mt-1 flex justify-between">
            <span className="text-muted-foreground">주문 가능 현금</span>
            <span className="tabular-nums">{formatKoreanUnit(availableCash)}원</span>
          </div>
        </div>

        {confirming ? (
          <div className="space-y-2 rounded border border-border bg-muted/30 p-3 text-xs">
            <p className="font-medium">
              {stockName} {qtyNumber}주를 {side === "BUY" ? "매수" : "매도"}하시겠습니까?
            </p>
            <p className="text-muted-foreground">
              예상 금액 {formatCurrency(estimatedAmount)}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setConfirming(false)}
                disabled={pending}
                className="flex-1 rounded border border-border py-2 font-medium hover:bg-background"
              >
                취소
              </button>
              <button
                type="button"
                onClick={confirmOrder}
                disabled={pending}
                className={cn(
                  "flex-1 rounded py-2 font-semibold transition disabled:opacity-60",
                  submitColor,
                )}
              >
                {pending ? "처리 중..." : "확인"}
              </button>
            </div>
          </div>
        ) : (
          <button
            type="button"
            onClick={handleClickSubmit}
            disabled={!canSubmit}
            className={cn(
              "w-full rounded py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",
              submitColor,
            )}
          >
            {side === "BUY" ? "매수 주문" : "매도 주문"}
          </button>
        )}

        <p className="text-center text-[10px] text-muted-foreground">
          모의 주문입니다. 실제 체결되지 않습니다.
        </p>
      </div>
    </div>
  );
}
