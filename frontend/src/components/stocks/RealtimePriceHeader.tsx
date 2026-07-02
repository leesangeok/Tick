"use client";

import { useMemo } from "react";
import { useMarketTick } from "@/hooks/useMarketTick";
import {
  formatCurrency,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

type Props = {
  symbol: string;
  initialPrice: number;
  initialChangeAmount: number;
  initialChangeRate: number;
};

/**
 * 종목 상세 페이지 우측 상단 가격 표시.
 *
 * 서버가 SSR 로 fetchStock() 결과에서 넘긴 초기값을 즉시 그림 → 첫 페인트에 이미 가격이 보임.
 * 마운트되면 `useMarketTick` 이 WS 붙어서 tick 을 받고, 도착한 순간부터 라이브 값으로 대체.
 * KIS tick 이 15분간 안 오면 (장 마감 등) 서버가 준 값이 계속 유지.
 *
 * 라이브 인디케이터: tick 이 한 번이라도 온 뒤부터 초록 점 + `LIVE` 라벨.
 */
export function RealtimePriceHeader({
  symbol,
  initialPrice,
  initialChangeAmount,
  initialChangeRate,
}: Props) {
  const tick = useMarketTick(symbol);

  const view = useMemo(() => {
    if (tick) {
      return {
        price: tick.price,
        changeAmount: tick.changeAmount,
        changeRate: tick.changeRate,
        live: true,
      };
    }
    return {
      price: initialPrice,
      changeAmount: initialChangeAmount,
      changeRate: initialChangeRate,
      live: false,
    };
  }, [tick, initialPrice, initialChangeAmount, initialChangeRate]);

  return (
    <div className="text-right">
      <div className="flex items-center justify-end gap-2">
        {view.live && (
          <span
            className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-medium text-emerald-500"
            aria-label="실시간 시세 스트리밍 중"
          >
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
            LIVE
          </span>
        )}
        <p className="text-3xl font-bold tabular-nums">{formatCurrency(view.price)}</p>
      </div>
      <p
        className={cn(
          "mt-1 text-sm font-medium tabular-nums",
          priceDirectionClass(view.changeAmount),
        )}
      >
        {priceArrow(view.changeAmount)}{" "}
        {formatSignedCurrency(view.changeAmount)} ({formatSignedPercent(view.changeRate)})
      </p>
    </div>
  );
}
