import { NextResponse, type NextRequest } from "next/server";
import { isAccessFresh } from "@/lib/jwt";

const PROTECTED_PREFIXES = [
  "/dashboard",
  "/portfolio",
  "/orders",
  "/ai-report",
];

const STOCK_DETAIL_PATTERN = /^\/stocks\/[^/]+/;

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const ACCESS_COOKIE = "tick_at";
const REFRESH_COOKIE = "tick_rt";

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const requiresAuth =
    PROTECTED_PREFIXES.some((p) => pathname.startsWith(p)) ||
    STOCK_DETAIL_PATTERN.test(pathname);

  if (!requiresAuth) return NextResponse.next();

  const access = request.cookies.get(ACCESS_COOKIE)?.value;
  if (isAccessFresh(access)) return NextResponse.next();

  // access 만료 or 없음 → refresh 시도
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  if (refresh) {
    const refreshRes = await fetch(`${API_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { Cookie: `${REFRESH_COOKIE}=${refresh}` },
      cache: "no-store",
    });
    if (refreshRes.ok) {
      const setCookieHeaders = refreshRes.headers.getSetCookie();
      const newAccess = parseAccessFromSetCookie(setCookieHeaders);

      // server component 도 새 access 를 보도록 incoming request 의 cookie 헤더를 갱신
      const requestHeaders = new Headers(request.headers);
      if (newAccess) {
        const cookiePairs = request.cookies
          .getAll()
          .map((c) => (c.name === ACCESS_COOKIE ? `${c.name}=${newAccess}` : `${c.name}=${c.value}`));
        if (!cookiePairs.some((p) => p.startsWith(`${ACCESS_COOKIE}=`))) {
          cookiePairs.push(`${ACCESS_COOKIE}=${newAccess}`);
        }
        requestHeaders.set("cookie", cookiePairs.join("; "));
      }

      const response = NextResponse.next({ request: { headers: requestHeaders } });
      // 브라우저에 새 access 쿠키 forward
      setCookieHeaders.forEach((c) => response.headers.append("set-cookie", c));
      return response;
    }
  }

  // refresh 도 실패 → 로그인 페이지로
  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("redirect", pathname);
  return NextResponse.redirect(loginUrl);
}

function parseAccessFromSetCookie(setCookieHeaders: string[]): string | null {
  for (const c of setCookieHeaders) {
    const m = c.match(/^tick_at=([^;]*)/);
    if (m && m[1]) return m[1];
  }
  return null;
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/portfolio/:path*",
    "/orders/:path*",
    "/ai-report/:path*",
    "/stocks/:symbol",
  ],
};
