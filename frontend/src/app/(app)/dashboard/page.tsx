import Link from "next/link";
import { Sparkles } from "lucide-react";
import { fetchPortfolio } from "@/services/portfolioService";
import { fetchOrders } from "@/services/orderService";
import { mockAiReports } from "@/mocks/aiReports";
import { mockDailyAssets } from "@/mocks/dailyAssets";
import { DashboardKpis } from "@/components/dashboard/DashboardKpis";
import { DailyAssetChart } from "@/components/dashboard/DailyAssetChart";
import { DepositButton } from "@/components/account/DepositButton";
import {
  formatCurrency,
  formatRelativeTime,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

export default async function DashboardPage() {
  const [portfolio, orders] = await Promise.all([
    fetchPortfolio(),
    fetchOrders(),
  ]);
  const recentOrders = orders.slice(0, 4);
  const dailySummary = mockAiReports.find((r) => r.type === "DAILY_SUMMARY");

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">대시보드</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            오늘의 자산 현황과 포트폴리오를 한눈에 확인하세요.
          </p>
        </div>
        <DepositButton />
      </div>

      <DashboardKpis portfolio={portfolio} />

      <DailyAssetChart data={mockDailyAssets} />

      <section className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card title="보유 종목" action={<Link href="/portfolio" className="text-xs text-muted-foreground hover:text-foreground">전체 보기 →</Link>}>
            <div className="divide-y divide-border">
              <div className="grid grid-cols-12 gap-2 px-4 py-2 text-xs text-muted-foreground">
                <div className="col-span-4">종목</div>
                <div className="col-span-2 text-right">수량</div>
                <div className="col-span-3 text-right">평가금액</div>
                <div className="col-span-3 text-right">수익률</div>
              </div>
              {portfolio.holdings.map((h) => (
                <Link
                  key={h.symbol}
                  href={`/stocks/${h.symbol}`}
                  className="grid grid-cols-12 items-center gap-2 px-4 py-3 text-sm hover:bg-accent/50"
                >
                  <div className="col-span-4">
                    <p className="font-medium">{h.name}</p>
                    <p className="text-xs text-muted-foreground">{h.symbol}</p>
                  </div>
                  <div className="col-span-2 text-right tabular-nums">
                    {h.quantity}주
                  </div>
                  <div className="col-span-3 text-right tabular-nums">
                    {formatCurrency(h.evaluationAmount)}
                  </div>
                  <div className={cn("col-span-3 text-right tabular-nums", priceDirectionClass(h.profitLoss))}>
                    <div className="font-medium">
                      {priceArrow(h.profitLoss)} {formatSignedPercent(h.profitRate)}
                    </div>
                    <div className="text-xs">
                      {formatSignedCurrency(h.profitLoss)}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card
            title="AI 포트폴리오 요약"
            badge={<Sparkles className="h-3.5 w-3.5 text-gain" />}
          >
            <div className="p-4 text-sm leading-relaxed">
              {dailySummary?.summary ??
                "오늘의 시장 요약이 아직 생성되지 않았습니다."}
            </div>
            {dailySummary && (
              <div className="border-t border-border px-4 py-3">
                <p className="mb-2 text-xs font-medium text-muted-foreground">
                  근거
                </p>
                <ul className="space-y-1">
                  {dailySummary.evidences.map((e, i) => (
                    <li key={i} className="text-xs text-muted-foreground">
                      · {e.title}{" "}
                      <span className="text-muted-foreground/60">({e.source})</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </Card>

          <Card title="최근 거래" action={<Link href="/orders" className="text-xs text-muted-foreground hover:text-foreground">전체 보기 →</Link>}>
            <ul className="divide-y divide-border">
              {recentOrders.map((o) => (
                <li
                  key={o.id}
                  className="flex items-center justify-between px-4 py-3 text-sm"
                >
                  <div>
                    <p className="font-medium">{o.stockName}</p>
                    <p className="text-xs text-muted-foreground">
                      {formatRelativeTime(o.createdAt)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p
                      className={cn(
                        "text-xs font-medium",
                        o.side === "BUY" ? "text-gain" : "text-loss",
                      )}
                    >
                      {o.side === "BUY" ? "매수" : "매도"} {o.quantity}주
                    </p>
                    <p className="text-xs text-muted-foreground tabular-nums">
                      {formatCurrency(o.price)}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </section>
    </div>
  );
}

function Card({
  title,
  action,
  badge,
  children,
}: {
  title: string;
  action?: React.ReactNode;
  badge?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-lg border border-border bg-card">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold">
          {badge}
          {title}
        </h2>
        {action}
      </header>
      <div>{children}</div>
    </section>
  );
}
