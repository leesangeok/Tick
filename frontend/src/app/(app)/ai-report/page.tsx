import { Sparkles } from "lucide-react";
import { fetchPortfolio } from "@/services/portfolioService";
import { LazyAiSummary } from "@/components/ai/LazyAiSummary";

export default async function AiReportPage() {
  const portfolio = await fetchPortfolio();
  const holdings = portfolio.holdings;

  // 보유 종목별 AI 요약을 client-side lazy fetch 로 분리.
  // 종목 수 × LLM 호출 시간이 SSR 을 차단하던 게 가장 큰 느림 원인이었음.

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">AI 리포트</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          내 보유 종목에 대한 AI 가 생성한 분석 요약을 한곳에서 확인합니다.
        </p>
      </div>

      {holdings.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-10 text-center">
          <Sparkles className="mx-auto mb-3 h-6 w-6 text-muted-foreground/60" />
          <p className="text-sm text-muted-foreground">
            아직 보유 중인 종목이 없습니다.
          </p>
          <p className="mt-1 text-xs text-muted-foreground/70">
            관심 있는 종목을 찾아 첫 모의투자를 시작해 보세요.
          </p>
        </div>
      ) : (
        <section className="grid gap-6 lg:grid-cols-2">
          {holdings.map((h) => (
            <LazyAiSummary
              key={h.symbol}
              symbol={h.symbol}
              stockName={h.name}
              compact
            />
          ))}
        </section>
      )}
    </div>
  );
}
