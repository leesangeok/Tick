import Link from "next/link";
import { ArrowRight, Sparkles, TrendingUp, Wallet } from "lucide-react";

export default function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex h-16 items-center justify-between px-6 md:px-12">
        <Link href="/" className="text-xl font-bold tracking-tight">
          Tick<span className="text-gain">.</span>
        </Link>
        <Link
          href="/dashboard"
          className="text-sm font-medium text-muted-foreground hover:text-foreground"
        >
          대시보드 →
        </Link>
      </header>

      <main className="flex flex-1 flex-col items-center justify-center px-6 py-16 text-center md:py-24">
        <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs text-muted-foreground">
          <Sparkles className="h-3 w-3 text-gain" />
          AI 모의투자 플랫폼
        </span>

        <h1 className="max-w-3xl text-4xl font-bold tracking-tight md:text-6xl">
          실시간 시세 기반
          <br />
          <span className="text-gain">AI 모의투자</span> 플랫폼
        </h1>

        <p className="mt-6 max-w-xl text-base text-muted-foreground md:text-lg">
          가상 자산으로 투자 전략을 실험하고, AI가 주가 변동 이유를 근거와 함께
          요약해줍니다.
        </p>

        <div className="mt-10 flex flex-col items-center gap-3 sm:flex-row">
          <Link
            href="/dashboard"
            className="inline-flex h-11 items-center gap-2 rounded-md bg-primary px-6 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90"
          >
            모의투자 시작하기
            <ArrowRight className="h-4 w-4" />
          </Link>
          <Link
            href="/stocks"
            className="inline-flex h-11 items-center rounded-md border border-border px-6 text-sm font-medium hover:bg-accent"
          >
            종목 둘러보기
          </Link>
        </div>

        <div className="mt-20 grid w-full max-w-4xl gap-4 md:grid-cols-3">
          <FeatureCard
            icon={<TrendingUp className="h-5 w-5" />}
            title="실시간 모의투자"
            description="실제 시세 흐름 기반의 가상 매수·매도. 손실 위험 없이 전략을 실험합니다."
          />
          <FeatureCard
            icon={<Sparkles className="h-5 w-5" />}
            title="AI 주가 분석"
            description="뉴스·공시·가격 변동을 근거로 종목 상승/하락 이유를 요약해 줍니다."
          />
          <FeatureCard
            icon={<Wallet className="h-5 w-5" />}
            title="포트폴리오 관리"
            description="자산·수익률·자산 비중을 한눈에 확인하고 거래 내역을 추적합니다."
          />
        </div>
      </main>

      <footer className="border-t border-border px-6 py-6 text-center text-xs text-muted-foreground md:px-12">
        © Tick. 모의투자 데이터는 실제 투자 권유가 아닙니다.
      </footer>
    </div>
  );
}

function FeatureCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-6 text-left">
      <div className="flex h-9 w-9 items-center justify-center rounded-md bg-accent text-foreground">
        {icon}
      </div>
      <h3 className="mt-4 text-base font-semibold">{title}</h3>
      <p className="mt-2 text-sm text-muted-foreground">{description}</p>
    </div>
  );
}
