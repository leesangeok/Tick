import type { DailyAssetPoint } from "@/types/account";
import { formatKoreanUnit } from "@/lib/format";

type Props = {
  data: DailyAssetPoint[];
};

export function DailyAssetChart({ data }: Props) {
  if (data.length === 0) return null;
  const min = Math.min(...data.map((d) => d.totalAssets));
  const max = Math.max(...data.map((d) => d.totalAssets));
  const range = max - min || 1;
  const last = data[data.length - 1]!;
  const first = data[0]!;
  const direction = last.totalAssets >= first.totalAssets ? "gain" : "loss";

  const w = 600;
  const h = 160;
  const points = data
    .map((d, i) => {
      const x = (i / (data.length - 1)) * w;
      const y = h - ((d.totalAssets - min) / range) * (h - 20) - 10;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold">자산 추이 (30일)</h2>
        <span className="text-xs text-muted-foreground tabular-nums">
          {formatKoreanUnit(min)}원 ~ {formatKoreanUnit(max)}원
        </span>
      </div>
      <svg
        viewBox={`0 0 ${w} ${h}`}
        className="h-40 w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label="최근 30일 총 자산 추이"
      >
        <defs>
          <linearGradient id="dailyAreaFill" x1="0" y1="0" x2="0" y2="1">
            <stop
              offset="0%"
              stopColor={direction === "gain" ? "hsl(var(--gain))" : "hsl(var(--loss))"}
              stopOpacity="0.25"
            />
            <stop
              offset="100%"
              stopColor={direction === "gain" ? "hsl(var(--gain))" : "hsl(var(--loss))"}
              stopOpacity="0"
            />
          </linearGradient>
        </defs>
        <polygon
          fill="url(#dailyAreaFill)"
          points={`0,${h} ${points} ${w},${h}`}
        />
        <polyline
          fill="none"
          stroke={direction === "gain" ? "hsl(var(--gain))" : "hsl(var(--loss))"}
          strokeWidth="2"
          points={points}
        />
      </svg>
    </div>
  );
}
