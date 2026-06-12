"use client";

import { useMemo } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { PricePoint } from "@/types/stock";
import { formatCurrency } from "@/lib/format";

type Props = {
  series: PricePoint[];
  changeAmount: number;
  ariaLabel: string;
};

export function StockChart({ series, changeAmount, ariaLabel }: Props) {
  const data = useMemo(
    () =>
      series.map((p) => ({
        date: p.timestamp.slice(5), // MM-DD (10자 ISO 의 5~10)
        close: p.close,
      })),
    [series],
  );

  if (data.length === 0) {
    return (
      <div className="flex h-44 w-full items-center justify-center rounded border border-dashed border-border text-xs text-muted-foreground">
        가격 데이터가 없습니다.
      </div>
    );
  }

  const strokeColor =
    changeAmount >= 0 ? "hsl(var(--gain))" : "hsl(var(--loss))";

  return (
    <div className="h-44 w-full" role="img" aria-label={ariaLabel}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid
            strokeDasharray="3 3"
            stroke="hsl(var(--border))"
            opacity={0.4}
            vertical={false}
          />
          <XAxis
            dataKey="date"
            tick={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
            axisLine={false}
            tickLine={false}
            minTickGap={20}
          />
          <YAxis
            domain={["dataMin", "dataMax"]}
            tick={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
            axisLine={false}
            tickLine={false}
            tickFormatter={(v: number) => formatCurrency(v)}
            width={70}
          />
          <Tooltip
            contentStyle={{
              background: "hsl(var(--card))",
              border: "1px solid hsl(var(--border))",
              borderRadius: 6,
              fontSize: 12,
              color: "hsl(var(--foreground))",
            }}
            labelStyle={{ color: "hsl(var(--muted-foreground))", fontSize: 10 }}
            formatter={(value) => [
              typeof value === "number" ? formatCurrency(value) : String(value),
              "종가",
            ]}
          />
          <Line
            type="monotone"
            dataKey="close"
            stroke={strokeColor}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
