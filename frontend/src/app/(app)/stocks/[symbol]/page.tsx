import { notFound } from "next/navigation";
import { Sparkles, Star } from "lucide-react";
import { fetchPriceSeries, fetchStock } from "@/services/stockService";
import { fetchAccount } from "@/services/accountService";
import { mockNews } from "@/mocks/news";
import { mockAiReports } from "@/mocks/aiReports";
import { OrderPanel } from "@/components/trading/OrderPanel";
import { StockChart } from "@/components/stocks/StockChart";
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
  const [stock, series, account] = await Promise.all([
    fetchStock(symbol),
    fetchPriceSeries(symbol, 60).catch(() => []),
    fetchAccount().catch(() => null),
  ]);
  if (!stock) notFound();
  const news = mockNews.filter((n) => n.symbol === symbol);
  const aiReport = mockAiReports.find(
    (r) => r.symbol === symbol && r.type !== "DAILY_SUMMARY",
  );

  const min = series.length > 0 ? Math.min(...series.map((p) => p.low)) : 0;
  const max = series.length > 0 ? Math.max(...series.map((p) => p.high)) : 0;

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
            <StockChart
              series={series}
              changeAmount={stock.changeAmount}
              ariaLabel={`${stock.name} 60일 가격 추이`}
            />
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
          <OrderPanel
            stockCode={stock.symbol}
            stockName={stock.name}
            currentPrice={stock.currentPrice}
            availableCash={account?.cash ?? 0}
          />
        </aside>
      </section>
    </div>
  );
}
