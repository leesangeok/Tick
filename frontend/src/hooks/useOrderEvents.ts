"use client";

import { useEffect } from "react";
import { subscribeToOrders, type OrderEvent } from "@/services/ordersWs";

/**
 * 실시간 주문 이벤트를 리스너로 받는다. 마운트 시 subscribe, 언마운트 시 unsubscribe.
 *
 * 대시보드/포트폴리오 페이지가 자기 리스너로 사용:
 *   useOrderEvents((ev) => refetchOrders())
 *
 * SSR 안전 — subscribeToOrders 는 window 없으면 no-op.
 */
export function useOrderEvents(onEvent: (event: OrderEvent) => void): void {
  useEffect(() => {
    return subscribeToOrders(onEvent);
  }, [onEvent]);
}
