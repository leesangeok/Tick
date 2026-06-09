import { notFound } from "next/navigation";
import { Sparkles, Star } from "lucide-react";
import { fetchPriceSeries, fetchStock } from "@/services/stockService";
import { mockNews } from "@/mocks/news";
import { mockAiReports } from "@/mocks/aiReports";
import { AvailableCash } from "@/components/account/AvailableCash";
import {
  formatCurrency,
  formatRelativeTime,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

type PageProps = {
  params: Promise<{ symbol: string }>;
};

export default async function StockDetailPage({ params }: PageProps) {
  const { symbol } = await params;
  const [stock, series] = await Promise.all([
    fetchStock(symbol),
    fetchPriceSeries(symbol, 60).catch(() => []),
  ]);
  if (!stock) notFound();
  const news = mockNews.filter((n) => n.symbol === symbol);
  const aiReport = mockAiReports.find(
    (r) => r.symbol === symbol && r.type !== "DAILY_SUMMARY",
  );

  const min = Math.min(...series.map((p) => p.low));
  const max = Math.max(...series.map((p) => p.high));
  const range = max - min || 1;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight">{stock.name}</h1>
            <button
              type="button"
              className="text-muted-foreground hover:text-foreground"
              aria-label="관심 종목"
            >
              <Star
                className={cn(
                  "h-5 w-5",
                  stock.isFavorite && "fill-gain text-gain",
                )}
              />
            </button>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {stock.symbol} · {stock.market} · {stock.sector}
          </p>
        </div>
        <div className="text-right">
          <p className="text-3xl font-bold tabular-nums">
            {formatCurrency(stock.currentPrice)}
          </p>
          <p
            className={cn(
              "mt-1 text-sm font-medium tabular-nums",
              priceDirectionClass(stock.changeAmount),
            )}
          >
            {priceArrow(stock.changeAmount)}{" "}
            {formatSignedCurrency(stock.changeAmount)} ({formatSignedPercent(stock.changeRate)})
          </p>
        </div>
      </header>

      <section className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-lg border border-border bg-card p-4">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold">최근 60일 가격</h2>
              <span className="text-xs text-muted-foreground tabular-nums">
                고가 {formatCurrency(max)} · 저가 {formatCurrency(min)}
              </span>
            </div>
            <svg
              viewBox="0 0 600 200"
              className="h-44 w-full"
              preserveAspectRatio="none"
              role="img"
              aria-label={`${stock.name} 60일 가격 추이`}
            >
              <polyline
                fill="none"
                stroke={stock.changeAmount >= 0 ? "hsl(var(--gain))" : "hsl(var(--loss))"}
                strokeWidth="2"
                points={series
                  .map((p, i) => {
                    const x = (i / (series.length - 1)) * 600;
                    const y = 200 - ((p.close - min) / range) * 180 - 10;
                    return `${x},${y}`;
                  })
                  .join(" ")}
              />
            </svg>
          </div>

          {aiReport && (
            <div className="rounded-lg border border-border bg-card">
              <header className="flex items-center gap-2 border-b border-border px-4 py-3">
                <Sparkles className="h-4 w-4 text-gain" />
                <h2 className="text-sm font-semibold">
                  AI {aiReport.type === "RISE_REASON" ? "상승" : "하락"} 이유 요약
                </h2>
              </header>
              <div className="p-4 text-sm leading-relaxed">
                {aiReport.summary}
              </div>
              <div className="border-t border-border px-4 py-3">
                <p className="mb-2 text-xs font-medium text-muted-foreground">
                  근거
                </p>
                <ul className="space-y-1">
                  {aiReport.evidences.map((e, i) => (
                    <li key={i} className="text-xs text-muted-foreground">
                      · {e.title}{" "}
                      <span className="text-muted-foreground/60">
                        ({e.source} · {formatRelativeTime(e.publishedAt)})
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {news.length > 0 && (
            <div className="rounded-lg border border-border bg-card">
              <header className="border-b border-border px-4 py-3">
                <h2 className="text-sm font-semibold">관련 뉴스</h2>
              </header>
              <ul className="divide-y divide-border">
                {news.map((n) => (
                  <li key={n.id} className="px-4 py-3">
                    <p className="text-sm font-medium">{n.title}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {n.summary}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground/70">
                      {n.source} · {formatRelativeTime(n.publishedAt)}
                    </p>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <aside className="lg:sticky lg:top-20 lg:self-start">
          <div className="rounded-lg border border-border bg-card">
            <div className="grid grid-cols-2 border-b border-border">
              <button
                type="button"
                className="border-r border-border bg-gain/10 px-4 py-3 text-sm font-semibold text-gain"
              >
                매수
              </button>
              <button
                type="button"
                className="px-4 py-3 text-sm font-medium text-muted-foreground hover:text-foreground"
              >
                매도
              </button>
            </div>
            <div className="space-y-4 p-4">
              <div className="flex gap-2 text-xs">
                <button className="flex-1 rounded border border-foreground bg-foreground py-1.5 font-medium text-background">
                  지정가
                </button>
                <button className="flex-1 rounded border border-border py-1.5 text-muted-foreground hover:text-foreground">
                  시장가
                </button>
              </div>
              <div>
                <label className="text-xs text-muted-foreground">수량</label>
                <input
                  type="number"
                  placeholder="0"
                  className="mt-1 w-full rounded border border-border bg-background px-3 py-2 text-right text-sm tabular-nums outline-none focus:border-foreground"
                />
              </div>
              <div>
                <label className="text-xs text-muted-foreground">주문 가격</label>
                <input
                  type="number"
                  defaultValue={stock.currentPrice}
                  className="mt-1 w-full rounded border border-border bg-background px-3 py-2 text-right text-sm tabular-nums outline-none focus:border-foreground"
                />
              </div>
              <div className="rounded bg-muted/50 p-3 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">예상 주문 금액</span>
                  <span className="tabular-nums">0원</span>
                </div>
                <div className="mt-1 flex justify-between">
                  <span className="text-muted-foreground">주문 가능</span>
                  <AvailableCash />
                </div>
              </div>
              <button
                type="button"
                className="w-full rounded bg-gain py-2.5 text-sm font-semibold text-white hover:opacity-90"
              >
                매수 주문
              </button>
              <p className="text-center text-[10px] text-muted-foreground">
                모의 주문입니다. 실제 체결되지 않습니다.
              </p>
            </div>
          </div>
        </aside>
      </section>
    </div>
  );
}
