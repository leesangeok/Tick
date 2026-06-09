import Link from "next/link";
import { fetchPortfolio } from "@/services/portfolioService";
import { fetchTransactions } from "@/services/transactionService";
import { DepositButton } from "@/components/account/DepositButton";
import { PortfolioSummary } from "@/components/portfolio/PortfolioSummary";
import { TransactionsList } from "@/components/portfolio/TransactionsList";
import {
  formatCurrency,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

export default async function PortfolioPage() {
  const [portfolio, transactions] = await Promise.all([
    fetchPortfolio(),
    fetchTransactions(),
  ]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">포트폴리오</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            보유 종목, 자산 비중, 거래 내역을 확인하세요.
          </p>
        </div>
        <DepositButton />
      </div>

      <PortfolioSummary portfolio={portfolio} />

      <section className="grid gap-6 lg:grid-cols-3">
        <div className="overflow-hidden rounded-lg border border-border bg-card lg:col-span-2">
          <header className="border-b border-border px-4 py-3">
            <h2 className="text-sm font-semibold">보유 종목</h2>
          </header>
          <table className="w-full text-sm">
            <thead className="text-xs text-muted-foreground">
              <tr>
                <th className="px-4 py-2 text-left font-medium">종목</th>
                <th className="px-4 py-2 text-right font-medium">수량</th>
                <th className="px-4 py-2 text-right font-medium">평균 단가</th>
                <th className="px-4 py-2 text-right font-medium">평가 금액</th>
                <th className="px-4 py-2 text-right font-medium">수익률</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {portfolio.holdings.map((h) => (
                <tr key={h.symbol} className="hover:bg-accent/40">
                  <td className="px-4 py-3">
                    <Link href={`/stocks/${h.symbol}`}>
                      <p className="font-medium">{h.name}</p>
                      <p className="text-xs text-muted-foreground">{h.symbol}</p>
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {h.quantity}주
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
                    {formatCurrency(h.averagePrice)}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {formatCurrency(h.evaluationAmount)}
                  </td>
                  <td
                    className={cn(
                      "px-4 py-3 text-right tabular-nums font-medium",
                      priceDirectionClass(h.profitLoss),
                    )}
                  >
                    <div>
                      {priceArrow(h.profitLoss)}{" "}
                      {formatSignedPercent(h.profitRate)}
                    </div>
                    <div className="text-xs">
                      {formatSignedCurrency(h.profitLoss)}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="rounded-lg border border-border bg-card">
          <header className="border-b border-border px-4 py-3">
            <h2 className="text-sm font-semibold">자산 비중</h2>
          </header>
          <div className="space-y-3 p-4">
            {portfolio.holdings.map((h) => {
              const ratio =
                portfolio.evaluationAmount > 0
                  ? (h.evaluationAmount / portfolio.evaluationAmount) * 100
                  : 0;
              return (
                <div key={h.symbol}>
                  <div className="flex justify-between text-xs">
                    <span className="font-medium">{h.name}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {ratio.toFixed(1)}%
                    </span>
                  </div>
                  <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full bg-foreground/70"
                      style={{ width: `${ratio}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      <section className="rounded-lg border border-border bg-card">
        <header className="border-b border-border px-4 py-3">
          <h2 className="text-sm font-semibold">최근 거래 / 입출금</h2>
        </header>
        <TransactionsList transactions={transactions} />
      </section>
    </div>
  );
}
