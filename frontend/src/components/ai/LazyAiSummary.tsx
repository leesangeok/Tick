"use client";

import { Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import {
  type AiSummaryResponse,
  fetchAiSummaryFromBrowser,
} from "@/services/aiSummaryClient";
import { AiSummaryCard } from "./AiSummaryCard";

type Props = {
  symbol: string;
  stockName: string;
  compact?: boolean;
};

type State =
  | { status: "loading" }
  | { status: "success"; summary: AiSummaryResponse }
  | { status: "empty" };

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * 종목 AI 요약을 브라우저에서 fetch. server component 에서 호출하면 LLM 호출 (1.5~4s)
 * 동안 페이지 SSR 이 차단되므로, 페이지 본체를 먼저 그리고 이 카드만 늦게 채워지도록 분리.
 *
 * fetch 실패 / 401 / 503 / 요약 미생성 등은 status="empty" 로 skeleton 자리에 안내 카드.
 */
export function LazyAiSummary({ symbol, stockName, compact = false }: Props) {
  const [state, setState] = useState<State>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    fetchAiSummaryFromBrowser(symbol, API_URL).then((summary) => {
      if (cancelled) return;
      setState(summary ? { status: "success", summary } : { status: "empty" });
    });
    return () => {
      cancelled = true;
    };
  }, [symbol]);

  if (state.status === "loading") {
    return <AiSummarySkeleton stockName={stockName} symbol={symbol} />;
  }
  if (state.status === "empty") {
    return <AiSummaryEmpty stockName={stockName} symbol={symbol} />;
  }
  return (
    <AiSummaryCard
      symbol={symbol}
      stockName={stockName}
      summary={state.summary}
      compact={compact}
    />
  );
}

function AiSummarySkeleton({
  stockName,
  symbol,
}: {
  stockName: string;
  symbol: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-card" aria-busy="true">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-gain animate-pulse" />
          <h3 className="text-sm font-semibold">{stockName}</h3>
          <span className="text-xs text-muted-foreground">{symbol}</span>
        </div>
        <span className="text-xs text-muted-foreground">AI 분석 생성 중…</span>
      </header>
      <div className="space-y-2 p-4">
        <div className="h-3 w-full animate-pulse rounded bg-muted/60" />
        <div className="h-3 w-11/12 animate-pulse rounded bg-muted/60" />
        <div className="h-3 w-9/12 animate-pulse rounded bg-muted/60" />
      </div>
    </div>
  );
}

function AiSummaryEmpty({
  stockName,
  symbol,
}: {
  stockName: string;
  symbol: string;
}) {
  return (
    <div className="rounded-lg border border-dashed border-border bg-card p-6">
      <div className="flex items-center gap-2">
        <Sparkles className="h-4 w-4 text-muted-foreground/60" />
        <h3 className="text-sm font-semibold">{stockName}</h3>
        <span className="text-xs text-muted-foreground">{symbol}</span>
      </div>
      <p className="mt-3 text-xs text-muted-foreground">
        AI 요약을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.
      </p>
    </div>
  );
}
