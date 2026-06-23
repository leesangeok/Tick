import { Sparkles } from "lucide-react";
import { fetchPortfolio } from "@/services/portfolioService";
import { fetchAiSummary } from "@/services/aiSummaryService";
import { AiSummaryCard } from "@/components/ai/AiSummaryCard";

export default async function AiReportPage() {
  const portfolio = await fetchPortfolio();
  const holdings = portfolio.holdings;

  // 보유 종목 별 AI 요약을 병렬 fetch. ai-server (LLM) 호출이라 종목 수에 비례한 비용/지연.
  const summaries = await Promise.all(
    holdings.map(async (h) => ({
      holding: h,
      summary: await fetchAiSummary(h.symbol).catch(() => null),
    })),
  );

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
          {summaries.map(({ holding, summary }) =>
            summary ? (
              <AiSummaryCard
                key={holding.symbol}
                symbol={holding.symbol}
                stockName={holding.name}
                summary={summary}
                compact
              />
            ) : (
              <div
                key={holding.symbol}
                className="rounded-lg border border-border bg-card p-6"
              >
                <div className="flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-muted-foreground/60" />
                  <h3 className="text-sm font-semibold">{holding.name}</h3>
                  <span className="text-xs text-muted-foreground">
                    {holding.symbol}
                  </span>
                </div>
                <p className="mt-3 text-xs text-muted-foreground">
                  AI 요약을 가져오지 못했습니다. 종목 상세에서 다시 확인해 주세요.
                </p>
              </div>
            ),
          )}
        </section>
      )}
    </div>
  );
}
