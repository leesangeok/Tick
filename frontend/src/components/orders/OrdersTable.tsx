"use client";

import { useMemo, useState } from "react";
import type { Order } from "@/types/order";
import {
  formatCurrency,
  formatRelativeTime,
  formatSignedCurrency,
  priceDirectionClass,
} from "@/lib/format";
import { cn } from "@/lib/utils";

type Filter = "ALL" | "BUY" | "SELL" | "PENDING" | "FILLED" | "CANCELED";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "BUY", label: "매수" },
  { value: "SELL", label: "매도" },
  { value: "PENDING", label: "대기" },
  { value: "FILLED", label: "체결" },
  { value: "CANCELED", label: "취소" },
];

const statusLabel = {
  PENDING: "대기",
  FILLED: "체결",
  CANCELED: "취소",
  REJECTED: "거절",
} as const;

const statusClass = {
  PENDING: "bg-muted text-muted-foreground",
  FILLED: "bg-gain/15 text-gain",
  CANCELED: "bg-muted text-muted-foreground",
  REJECTED: "bg-loss/15 text-loss",
} as const;

type Props = {
  orders: Order[];
};

export function OrdersTable({ orders }: Props) {
  const [filter, setFilter] = useState<Filter>("ALL");

  const filtered = useMemo(() => {
    return orders.filter((o) => {
      switch (filter) {
        case "BUY":
          return o.side === "BUY";
        case "SELL":
          return o.side === "SELL";
        case "PENDING":
          return o.status === "PENDING";
        case "FILLED":
          return o.status === "FILLED";
        case "CANCELED":
          return o.status === "CANCELED";
        default:
          return true;
      }
    });
  }, [orders, filter]);

  return (
    <div className="space-y-4">
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

      <div className="overflow-x-auto rounded-lg border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">시각</th>
              <th className="px-4 py-3 text-left font-medium">종목</th>
              <th className="px-4 py-3 text-center font-medium">구분</th>
              <th className="px-4 py-3 text-right font-medium">수량</th>
              <th className="px-4 py-3 text-right font-medium">주문 가격</th>
              <th className="px-4 py-3 text-right font-medium">실현 손익</th>
              <th className="px-4 py-3 text-center font-medium">상태</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-xs text-muted-foreground">
                  조건에 맞는 주문이 없습니다.
                </td>
              </tr>
            ) : (
              filtered.map((o) => (
                <tr key={o.id} className="hover:bg-accent/40">
                  <td className="px-4 py-3 text-xs text-muted-foreground">
                    {formatRelativeTime(o.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-medium">{o.stockName}</p>
                    <p className="text-xs text-muted-foreground">{o.symbol}</p>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span
                      className={cn(
                        "rounded px-2 py-0.5 text-xs font-medium",
                        o.side === "BUY" ? "bg-gain/15 text-gain" : "bg-loss/15 text-loss",
                      )}
                    >
                      {o.side === "BUY" ? "매수" : "매도"}{" "}
                      {o.orderType === "MARKET" ? "(시장)" : "(지정)"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">{o.quantity}주</td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {formatCurrency(o.price)}
                  </td>
                  <td
                    className={cn(
                      "px-4 py-3 text-right tabular-nums",
                      o.realizedProfitLoss != null
                        ? priceDirectionClass(o.realizedProfitLoss)
                        : "text-muted-foreground/40",
                    )}
                  >
                    {o.realizedProfitLoss != null
                      ? formatSignedCurrency(o.realizedProfitLoss)
                      : "—"}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span
                      className={cn(
                        "rounded-full px-2 py-0.5 text-xs",
                        statusClass[o.status],
                      )}
                    >
                      {statusLabel[o.status]}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
