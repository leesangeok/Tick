import Link from "next/link";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type SearchParams = Promise<{ redirect?: string; error?: string }>;

export default async function LoginPage({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const { error } = await searchParams;
  const kakaoLoginUrl = `${API_URL}/oauth2/authorization/kakao`;

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <Link href="/" className="block text-center text-2xl font-bold tracking-tight">
          Tick<span className="text-gain">.</span>
        </Link>
        <p className="mt-3 text-center text-sm text-muted-foreground">
          모의투자를 시작하려면 로그인해 주세요.
        </p>

        {error && (
          <p className="mt-4 rounded-md border border-loss/30 bg-loss/10 px-3 py-2 text-center text-xs text-loss">
            로그인에 실패했어요. 다시 시도해 주세요.
          </p>
        )}

        <a
          href={kakaoLoginUrl}
          className="mt-8 flex h-12 w-full items-center justify-center gap-2 rounded-md bg-[#FEE500] text-sm font-semibold text-[#191919] hover:opacity-90"
        >
          <KakaoIcon />
          카카오로 1초 로그인
        </a>

        <p className="mt-6 text-center text-[10px] text-muted-foreground">
          가입 시 가상 자산 1,000만원이 자동 지급됩니다.
          <br />
          실제 결제는 일어나지 않습니다.
        </p>
      </div>
    </div>
  );
}

function KakaoIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
      <path
        d="M9 1.5C4.58172 1.5 1 4.30558 1 7.76471C1 9.95 2.40828 11.8593 4.5 12.9706L3.5 16.5L7.10828 14.0265C7.71717 14.1191 8.34928 14.1765 9 14.1765C13.4183 14.1765 17 11.3709 17 7.91176C17 4.45264 13.4183 1.5 9 1.5Z"
        fill="#191919"
      />
    </svg>
  );
}
