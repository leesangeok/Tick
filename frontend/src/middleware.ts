import { NextResponse, type NextRequest } from "next/server";

const PROTECTED_PREFIXES = [
  "/dashboard",
  "/portfolio",
  "/orders",
  "/ai-report",
];

const STOCK_DETAIL_PATTERN = /^\/stocks\/[^/]+/;

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const requiresAuth =
    PROTECTED_PREFIXES.some((p) => pathname.startsWith(p)) ||
    STOCK_DETAIL_PATTERN.test(pathname);

  if (!requiresAuth) return NextResponse.next();

  const token = request.cookies.get("tick_at")?.value;
  if (token) return NextResponse.next();

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("redirect", pathname);
  return NextResponse.redirect(loginUrl);
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
