import { Sparkles, TrendingDown, TrendingUp } from "lucide-react";
import Link from "next/link";
import { mockAiReports } from "@/mocks/aiReports";
import { formatRelativeTime } from "@/lib/format";

export default function AiReportPage() {
  const daily = mockAiReports.find((r) => r.type === "DAILY_SUMMARY");
  const rise = mockAiReports.filter((r) => r.type === "RISE_REASON");
  const fall = mockAiReports.filter((r) => r.type === "FALL_REASON");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">AI 리포트</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          AI가 뉴스·공시·가격 변동을 바탕으로 시장과 종목 흐름을 요약합니다.
        </p>
      </div>

      {daily && (
        <section className="rounded-lg border border-border bg-card">
          <header className="flex items-center gap-2 border-b border-border px-4 py-3">
            <Sparkles className="h-4 w-4 text-gain" />
            <h2 className="text-sm font-semibold">오늘의 시장 요약</h2>
            <span className="ml-auto text-xs text-muted-foreground">
              {formatRelativeTime(daily.createdAt)}
            </span>
          </header>
          <div className="p-4 text-sm leading-relaxed">{daily.summary}</div>
          <div className="border-t border-border px-4 py-3">
            <p className="mb-2 text-xs font-medium text-muted-foreground">
              근거
            </p>
            <ul className="space-y-1">
              {daily.evidences.map((e, i) => (
                <li key={i} className="text-xs text-muted-foreground">
                  · {e.title}{" "}
                  <span className="text-muted-foreground/60">({e.source})</span>
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}

      <section className="grid gap-6 lg:grid-cols-2">
        <ReasonGroup
          title="상승 이유"
          tone="gain"
          icon={<TrendingUp className="h-4 w-4 text-gain" />}
          reports={rise}
        />
        <ReasonGroup
          title="하락 이유"
          tone="loss"
          icon={<TrendingDown className="h-4 w-4 text-loss" />}
          reports={fall}
        />
      </section>
    </div>
  );
}

function ReasonGroup({
  title,
  icon,
  reports,
}: {
  title: string;
  tone: "gain" | "loss";
  icon: React.ReactNode;
  reports: typeof mockAiReports;
}) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        {icon}
        <h2 className="text-sm font-semibold">{title}</h2>
      </div>
      {reports.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-6 text-center text-xs text-muted-foreground">
          해당하는 종목이 없습니다.
        </p>
      ) : (
        reports.map((r) => (
          <Link
            key={r.id}
            href={`/stocks/${r.symbol}`}
            className="block rounded-lg border border-border bg-card p-4 transition-colors hover:bg-accent/40"
          >
            <div className="flex items-center justify-between">
              <p className="font-medium">{r.stockName}</p>
              <span className="text-xs text-muted-foreground">{r.symbol}</span>
            </div>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {r.summary}
            </p>
            <p className="mt-2 text-xs text-muted-foreground/70">
              근거 {r.evidences.length}건 · {formatRelativeTime(r.createdAt)}
            </p>
          </Link>
        ))
      )}
    </div>
  );
}
