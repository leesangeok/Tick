import type { Portfolio } from "@/types/portfolio";
import {
  formatKoreanUnit,
  formatSignedCurrency,
  formatSignedPercent,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

type Props = {
  portfolio: Portfolio;
};

export function DashboardKpis({ portfolio: p }: Props) {
  return (
    <section className="grid grid-cols-2 gap-3 md:grid-cols-5">
      <KpiCard
        label="총 자산"
        value={`${formatKoreanUnit(p.totalAssets)}원`}
        sub={`총 입금 ${formatKoreanUnit(p.totalDeposits)}원`}
      />
      <KpiCard label="보유 현금" value={`${formatKoreanUnit(p.cash)}원`} />
      <KpiCard
        label="평가 손익"
        value={formatSignedCurrency(p.unrealizedProfitLoss)}
        sub={`평가금액 ${formatKoreanUnit(p.evaluationAmount)}원`}
        tone={p.unrealizedProfitLoss}
      />
      <KpiCard
        label="실현 손익"
        value={formatSignedCurrency(p.realizedProfitLoss)}
        sub="누적 매도 차익"
        tone={p.realizedProfitLoss}
      />
      <KpiCard
        label="총 수익률"
        value={formatSignedPercent(p.totalProfitRate)}
        sub={`오늘 ${formatSignedPercent(p.todayProfitRate)}`}
        tone={p.totalProfitLoss}
        subTone={p.todayProfitLoss}
      />
    </section>
  );
}

function KpiCard({
  label,
  value,
  sub,
  tone,
  subTone,
}: {
  label: string;
  value: string;
  sub?: string;
  tone?: number;
  subTone?: number;
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={cn(
          "mt-2 text-lg font-bold tabular-nums md:text-xl",
          tone !== undefined && priceDirectionClass(tone),
        )}
      >
        {value}
      </p>
      {sub && (
        <p
          className={cn(
            "mt-0.5 text-xs tabular-nums",
            subTone !== undefined
              ? priceDirectionClass(subTone)
              : "text-muted-foreground",
          )}
        >
          {sub}
        </p>
      )}
    </div>
  );
}
