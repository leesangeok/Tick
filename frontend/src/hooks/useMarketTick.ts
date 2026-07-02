"use client";

import { useEffect, useState } from "react";
import { subscribeToTicks, type MarketTick } from "@/services/marketWs";

/**
 * 특정 종목의 실시간 tick 을 구독. 마운트 시 subscribe, 언마운트 시 unsubscribe.
 *
 * SSR 안전: `useEffect` 안에서만 마켓 WS 매니저를 건드림.
 * symbol 변경 시 자동으로 이전 구독 해제 + 새 심볼 구독.
 *
 * `null` 반환 = 아직 tick 을 한 번도 못 받은 상태 → 호출부는 서버가 넘긴 초기값을 계속 표시.
 */
export function useMarketTick(symbol: string | undefined): MarketTick | null {
  const [tick, setTick] = useState<MarketTick | null>(null);

  useEffect(() => {
    if (!symbol) return;
    const unsubscribe = subscribeToTicks(symbol, (next) => {
      if (next.symbol === symbol) setTick(next);
    });
    return unsubscribe;
  }, [symbol]);

  // 심볼이 바뀐 직후엔 이전 심볼의 tick 이 잠깐 남아있을 수 있음 → 렌더 시점 필터.
  // setState(null) 을 effect 안에서 부르면 React 19 의 `set-state-in-effect` 룰 위반.
  return tick && tick.symbol === symbol ? tick : null;
}
