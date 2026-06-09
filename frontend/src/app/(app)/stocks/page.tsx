import Link from "next/link";
import { Star } from "lucide-react";
import { fetchStocks } from "@/services/stockService";
import {
  formatCurrency,
  formatKoreanUnit,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

export default async function StocksPage() {
  const stocks = await fetchStocks();
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">종목</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          투자 가능한 종목을 검색하고 시세를 확인하세요.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {["전체", "KOSPI", "KOSDAQ", "관심 종목", "상승", "하락"].map((tag, i) => (
          <button
            key={tag}
            type="button"
            className={cn(
              "rounded-full border px-3 py-1.5 text-xs font-medium",
              i === 0
                ? "border-foreground bg-foreground text-background"
                : "border-border text-muted-foreground hover:bg-accent hover:text-foreground",
            )}
          >
            {tag}
          </button>
        ))}
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">종목</th>
              <th className="px-4 py-3 text-right font-medium">현재가</th>
              <th className="px-4 py-3 text-right font-medium">전일 대비</th>
              <th className="px-4 py-3 text-right font-medium">등락률</th>
              <th className="hidden px-4 py-3 text-right font-medium md:table-cell">
                거래량
              </th>
              <th className="hidden px-4 py-3 text-center font-medium md:table-cell">
                시장
              </th>
              <th className="w-10 px-2 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {stocks.map((s) => (
              <tr key={s.symbol} className="hover:bg-accent/40">
                <td className="px-4 py-3">
                  <Link href={`/stocks/${s.symbol}`} className="block">
                    <p className="font-medium">{s.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {s.symbol} · {s.sector}
                    </p>
                  </Link>
                </td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {formatCurrency(s.currentPrice)}
                </td>
                <td
                  className={cn(
                    "px-4 py-3 text-right tabular-nums",
                    priceDirectionClass(s.changeAmount),
                  )}
                >
                  {formatSignedCurrency(s.changeAmount)}
                </td>
                <td
                  className={cn(
                    "px-4 py-3 text-right tabular-nums font-medium",
                    priceDirectionClass(s.changeRate),
                  )}
                >
                  {priceArrow(s.changeRate)} {formatSignedPercent(s.changeRate)}
                </td>
                <td className="hidden px-4 py-3 text-right tabular-nums text-muted-foreground md:table-cell">
                  {formatKoreanUnit(s.volume)}
                </td>
                <td className="hidden px-4 py-3 text-center text-xs text-muted-foreground md:table-cell">
                  {s.market}
                </td>
                <td className="px-2 py-3 text-center">
                  <Star
                    className={cn(
                      "h-4 w-4",
                      s.isFavorite
                        ? "fill-gain text-gain"
                        : "text-muted-foreground",
                    )}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
