"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useOrderEvents } from "@/hooks/useOrderEvents";
import type { OrderEvent } from "@/services/ordersWs";

/**
 * (app) 그룹 전역에서 `/ws/orders` 를 구독. 주문 체결 이벤트가 오면:
 * - 실현손익 부호에 따라 sonner toast (success/info/error) 로 알림
 * - router.refresh() 로 현재 페이지의 서버 컴포넌트 재조회 (orders/portfolio 자동 갱신)
 *
 * SSR-safe — 훅 내부 useEffect 에서만 window 접근.
 * 로그인/공개 페이지는 (app) 그룹 밖이라 이 리스너가 안 붙는다.
 */
export function OrderEventsListener() {
  const router = useRouter();

  const onOrderEvent = useCallback(
    (event: OrderEvent) => {
      toast(...toastFor(event));
      // 서버 컴포넌트 상 orders / portfolio 페이지가 최신 데이터로 다시 렌더.
      router.refresh();
    },
    [router],
  );

  useOrderEvents(onOrderEvent);
  return null;
}

type ToastArgs = [message: string, options?: { description?: string }];

function toastFor(event: OrderEvent): ToastArgs {
  const stock = `${event.stockName} ${event.quantity}주`;
  if (event.status !== "FILLED") {
    // PENDING/CANCELED/REJECTED. 정보용 표시.
    return [`${stock} · ${event.status.toLowerCase()}`];
  }
  const priceLine = `${event.price.toLocaleString()}원`;
  if (event.side === "BUY") {
    return [`매수 체결: ${stock}`, { description: priceLine }];
  }
  const pnl = event.realizedProfitLoss;
  if (pnl == null) {
    return [`매도 체결: ${stock}`, { description: priceLine }];
  }
  const sign = pnl >= 0 ? "+" : "";
  return [
    `매도 체결: ${stock}`,
    { description: `${priceLine} · ${sign}${pnl.toLocaleString()}원` },
  ];
}
