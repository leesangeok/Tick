"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Search, Star } from "lucide-react";
import { useHotkeys } from "react-hotkeys-hook";
import { toast } from "sonner";
import type { Stock } from "@/types/stock";
import { useWatchlistStore } from "@/stores/useWatchlistStore";
import {
  formatCurrency,
  formatKoreanUnit,
  formatSignedCurrency,
  formatSignedPercent,
  priceArrow,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

type Filter = "ALL" | "KOSPI" | "KOSDAQ" | "WATCHLIST" | "GAINERS" | "LOSERS";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "KOSPI", label: "KOSPI" },
  { value: "KOSDAQ", label: "KOSDAQ" },
  { value: "WATCHLIST", label: "관심 종목" },
  { value: "GAINERS", label: "상승" },
  { value: "LOSERS", label: "하락" },
];

type Props = {
  stocks: Stock[];
};

export function StockTable({ stocks }: Props) {
  const [filter, setFilter] = useState<Filter>("ALL");
  const [query, setQuery] = useState("");
  const watchlist = useWatchlistStore((s) => s.symbols);
  const toggleWatch = useWatchlistStore((s) => s.toggle);
  const hydrate = useWatchlistStore((s) => s.hydrate);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useHotkeys("/", (e) => {
    e.preventDefault();
    document.getElementById("stock-search")?.focus();
  });

  const watchSet = useMemo(() => new Set(watchlist), [watchlist]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return stocks.filter((s) => {
      if (q) {
        const hay = `${s.name} ${s.symbol} ${s.sector}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      switch (filter) {
        case "KOSPI":
          return s.market === "KOSPI";
        case "KOSDAQ":
          return s.market === "KOSDAQ";
        case "WATCHLIST":
          return watchSet.has(s.symbol);
        case "GAINERS":
          return s.changeAmount > 0;
        case "LOSERS":
          return s.changeAmount < 0;
        default:
          return true;
      }
    });
  }, [stocks, filter, query, watchSet]);

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex flex-wrap gap-2">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              onClick={() => setFilter(f.value)}
              className={cn(
                "rounded-full border px-3 py-1.5 text-xs font-medium transition",
                filter === f.value
                  ? "border-foreground bg-foreground text-background"
                  : "border-border text-muted-foreground hover:bg-accent hover:text-foreground",
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="relative md:w-64">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            id="stock-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="종목명 / 코드 / 업종 ( / 키로 포커스)"
            className="w-full rounded-md border border-border bg-background py-1.5 pl-9 pr-3 text-xs outline-none focus:border-foreground"
          />
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">종목</th>
              <th className="px-4 py-3 text-right font-medium">현재가</th>
              <th className="px-4 py-3 text-right font-medium">전일 대비</th>
              <th className="px-4 py-3 text-right font-medium">등락률</th>
              <th className="hidden px-4 py-3 text-right font-medium md:table-cell">거래량</th>
              <th className="hidden px-4 py-3 text-center font-medium md:table-cell">시장</th>
              <th className="w-10 px-2 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-xs text-muted-foreground">
                  조건에 맞는 종목이 없습니다.
                </td>
              </tr>
            ) : (
              filtered.map((s) => {
                const isFav = watchSet.has(s.symbol);
                return (
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
                      <button
                        type="button"
                        onClick={() => {
                          toggleWatch(s.symbol).catch(() => {
                            toast.error("로그인 후 이용할 수 있어요.");
                          });
                        }}
                        aria-label={isFav ? "관심 종목 해제" : "관심 종목 추가"}
                        aria-pressed={isFav}
                        className="rounded p-1 hover:bg-accent"
                      >
                        <Star
                          className={cn(
                            "h-4 w-4 transition",
                            isFav ? "fill-gain text-gain" : "text-muted-foreground",
                          )}
                        />
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
