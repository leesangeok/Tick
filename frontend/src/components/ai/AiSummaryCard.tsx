import Link from "next/link";
import { ArrowRight, Sparkles } from "lucide-react";
import { formatRelativeTime } from "@/lib/format";
import type { AiSummaryResponse } from "@/services/aiSummaryService";

type Props = {
  symbol: string;
  stockName: string;
  summary: AiSummaryResponse;
  /** 카드 안에 노출할 keyReasons / riskNotes / sources 의 최대 개수 (compact 모드에서 활용). */
  compact?: boolean;
};

/**
 * 종목별 AI 요약 카드. 종목 상세 페이지 / 대시보드 / AI 리포트 페이지에서 공통으로 사용.
 *
 * compact=true 면 keyReasons / riskNotes / sources 를 최대 2건씩만 보여주고
 * 종목 상세로 가는 "자세히" 링크를 노출 (목록형 화면 용).
 */
export function AiSummaryCard({ symbol, stockName, summary, compact = false }: Props) {
  const keyReasons = compact ? summary.keyReasons.slice(0, 2) : summary.keyReasons;
  const riskNotes = compact ? summary.riskNotes.slice(0, 2) : summary.riskNotes;
  const sources = compact ? summary.sources.slice(0, 3) : summary.sources;

  return (
    <div className="rounded-lg border border-border bg-card">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-gain" />
          <h3 className="text-sm font-semibold">{stockName}</h3>
          <span className="text-xs text-muted-foreground">{symbol}</span>
        </div>
        <span className="text-xs text-muted-foreground tabular-nums">
          뉴스 {summary.retrievedCount}건 분석
        </span>
      </header>

      <div className="p-4 text-sm leading-relaxed">{summary.summary}</div>

      {keyReasons.length > 0 && (
        <div className="border-t border-border px-4 py-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">핵심 요인</p>
          <ul className="space-y-1">
            {keyReasons.map((reason, i) => (
              <li key={i} className="text-xs text-muted-foreground">
                · {reason.text}
                {reason.sourceIndices.length > 0 && (
                  <span className="ml-1 inline-flex gap-1">
                    {reason.sourceIndices.map((idx) => {
                      const src = summary.sources[idx - 1];
                      if (!src) return null;
                      const label = `#${idx}`;
                      return src.sourceUrl ? (
                        <a
                          key={`${i}-${idx}`}
                          href={src.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          title={src.title}
                          className="rounded bg-muted px-1 text-[10px] tabular-nums text-muted-foreground hover:text-foreground"
                        >
                          {label}
                        </a>
                      ) : (
                        <span
                          key={`${i}-${idx}`}
                          title={src.title}
                          className="rounded bg-muted px-1 text-[10px] tabular-nums text-muted-foreground"
                        >
                          {label}
                        </span>
                      );
                    })}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {riskNotes.length > 0 && (
        <div className="border-t border-border px-4 py-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">리스크 노트</p>
          <ul className="space-y-1">
            {riskNotes.map((note, i) => (
              <li key={i} className="text-xs text-muted-foreground">
                · {note}
              </li>
            ))}
          </ul>
        </div>
      )}

      {sources.length > 0 && (
        <div className="border-t border-border px-4 py-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">근거</p>
          <ul className="space-y-1">
            {sources.map((s, i) => (
              <li key={i} className="text-xs text-muted-foreground">
                {s.sourceUrl ? (
                  <a
                    href={s.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-foreground"
                  >
                    · {s.title}
                  </a>
                ) : (
                  <span>· {s.title}</span>
                )}
                <span className="text-muted-foreground/60">
                  {" "}
                  ({s.source ?? "출처 미상"} · {formatRelativeTime(s.publishedAt)})
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {compact && (
        <Link
          href={`/stocks/${symbol}`}
          className="flex items-center justify-end gap-1 border-t border-border px-4 py-3 text-xs text-muted-foreground hover:text-foreground"
        >
          자세히 보기 <ArrowRight className="h-3 w-3" />
        </Link>
      )}
    </div>
  );
}
