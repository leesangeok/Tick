import {
  formatCurrency,
  formatRelativeTime,
  formatSignedCurrency,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import type { Transaction, TransactionType } from "@/types/account";

const typeLabel: Record<TransactionType, string> = {
  DEPOSIT: "입금",
  WITHDRAW: "출금",
  BUY: "매수",
  SELL: "매도",
};

const typeTone: Record<TransactionType, string> = {
  DEPOSIT: "bg-foreground/10 text-foreground",
  WITHDRAW: "bg-muted text-muted-foreground",
  BUY: "bg-gain/15 text-gain",
  SELL: "bg-loss/15 text-loss",
};

type Props = {
  transactions: Transaction[];
};

export function TransactionsList({ transactions }: Props) {
  if (transactions.length === 0) {
    return (
      <p className="px-4 py-8 text-center text-xs text-muted-foreground">
        아직 거래 내역이 없습니다.
      </p>
    );
  }

  return (
    <ul className="divide-y divide-border">
      {transactions.slice(0, 12).map((t) => (
        <li
          key={t.id}
          className="flex items-center justify-between px-4 py-3 text-sm"
        >
          <div className="flex items-center gap-3">
            <span
              className={cn(
                "rounded px-2 py-0.5 text-xs font-medium",
                typeTone[t.type],
              )}
            >
              {typeLabel[t.type]}
            </span>
            <div>
              <p className="font-medium">
                {t.stockName ?? (t.type === "DEPOSIT" ? "가상 자산 충전" : "출금")}
              </p>
              <p className="text-xs text-muted-foreground">
                {formatRelativeTime(t.createdAt)}
                {t.quantity != null && t.price != null && (
                  <>
                    {" · "}
                    {t.quantity}주 × {formatCurrency(t.price)}
                  </>
                )}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-medium tabular-nums">
              {t.type === "DEPOSIT" || t.type === "SELL" ? "+" : "-"}
              {formatCurrency(t.amount)}
            </p>
            {t.realizedProfitLoss != null && (
              <p
                className={cn(
                  "text-xs tabular-nums",
                  priceDirectionClass(t.realizedProfitLoss),
                )}
              >
                실현 {formatSignedCurrency(t.realizedProfitLoss)}
              </p>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
