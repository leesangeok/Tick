"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useHotkeys } from "react-hotkeys-hook";
import { useOrderStore } from "@/stores/useOrderStore";

/**
 * 전역 단축키 마운트 포인트.
 * - `/`        : 종목 검색창 포커스 (StockTable 안에서도 자체적으로 처리하지만
 *                 다른 페이지에서도 검색창이 있을 때 동작)
 * - `b` / `s`  : 매수/매도 탭 전환 (종목 상세에서 OrderPanel 이 store 를 구독)
 * - `g d/s/p/o/a` : 네비게이션 (g 키 누른 직후 두 번째 키)
 */
export function GlobalHotkeys() {
  const router = useRouter();
  const setSide = useOrderStore((s) => s.setSide);
  const [gPending, setGPending] = useState(false);

  useHotkeys("/", (e) => {
    e.preventDefault();
    document.getElementById("stock-search")?.focus();
  });

  useHotkeys("b", () => {
    setSide("BUY");
  });

  useHotkeys("s", () => {
    setSide("SELL");
  });

  // g 누르고 다음 키 기다림
  useHotkeys("g", (e) => {
    e.preventDefault();
    setGPending(true);
    window.setTimeout(() => setGPending(false), 800);
  });

  useHotkeys(
    "d",
    () => {
      if (!gPending) return;
      setGPending(false);
      router.push("/dashboard");
    },
    { enabled: gPending },
  );
  useHotkeys(
    "s",
    () => {
      if (!gPending) return;
      setGPending(false);
      router.push("/stocks");
    },
    { enabled: gPending },
  );
  useHotkeys(
    "p",
    () => {
      if (!gPending) return;
      setGPending(false);
      router.push("/portfolio");
    },
    { enabled: gPending },
  );
  useHotkeys(
    "o",
    () => {
      if (!gPending) return;
      setGPending(false);
      router.push("/orders");
    },
    { enabled: gPending },
  );
  useHotkeys(
    "a",
    () => {
      if (!gPending) return;
      setGPending(false);
      router.push("/ai-report");
    },
    { enabled: gPending },
  );

  return null;
}
