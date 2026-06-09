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

export function PortfolioSummary({ portfolio: p }: Props) {
  return (
    <div className="grid gap-3 md:grid-cols-5">
      <Card label="총 자산" value={`${formatKoreanUnit(p.totalAssets)}원`} />
      <Card label="보유 현금" value={`${formatKoreanUnit(p.cash)}원`} />
      <Card
        label="평가 손익"
        value={formatSignedCurrency(p.unrealizedProfitLoss)}
        tone={p.unrealizedProfitLoss}
      />
      <Card
        label="실현 손익"
        value={formatSignedCurrency(p.realizedProfitLoss)}
        tone={p.realizedProfitLoss}
      />
      <Card
        label="총 수익률"
        value={formatSignedPercent(p.totalProfitRate)}
        tone={p.totalProfitLoss}
        sub={`총 입금 ${formatKoreanUnit(p.totalDeposits)}원`}
      />
    </div>
  );
}

function Card({
  label,
  value,
  sub,
  tone,
}: {
  label: string;
  value: string;
  sub?: string;
  tone?: number;
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
        <p className="mt-0.5 text-xs text-muted-foreground tabular-nums">
          {sub}
        </p>
      )}
    </div>
  );
}
