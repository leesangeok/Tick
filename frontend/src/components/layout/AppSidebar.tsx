"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useEffect, useState, useTransition } from "react";
import {
  LayoutDashboard,
  Search,
  Wallet,
  FileText,
  Sparkles,
  LogOut,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { logout, type MeResponse } from "@/services/accountClient";

const navItems = [
  { href: "/dashboard", icon: LayoutDashboard, label: "대시보드" },
  { href: "/stocks", icon: Search, label: "종목" },
  { href: "/portfolio", icon: Wallet, label: "포트폴리오" },
  { href: "/orders", icon: FileText, label: "주문 내역" },
  { href: "/ai-report", icon: Sparkles, label: "AI 리포트" },
];

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function AppSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    fetch(`${API_URL}/api/v1/auth/me`, { credentials: "include", cache: "no-store" })
      .then((r) => (r.ok ? r.json() : null))
      .then((body) => setMe(body?.success ? body.data : null))
      .catch(() => setMe(null));
  }, []);

  async function handleLogout() {
    await logout();
    startTransition(() => router.replace("/login"));
  }

  return (
    <aside className="hidden border-r border-border bg-card md:flex md:w-60 md:flex-col">
      <div className="flex h-16 items-center px-6">
        <Link href="/" className="text-xl font-bold tracking-tight">
          Tick<span className="text-gain">.</span>
        </Link>
      </div>
      <nav className="flex flex-1 flex-col gap-1 px-3 py-4">
        {navItems.map(({ href, icon: Icon, label }) => {
          const active =
            pathname === href || pathname.startsWith(href + "/");
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors",
                active
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground hover:bg-accent/50 hover:text-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              {label}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-border p-4">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-full bg-muted" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">
              {me?.nickname ?? "로그인 필요"}
            </p>
            <p className="truncate text-xs text-muted-foreground">
              {me?.email ?? ""}
            </p>
          </div>
          {me && (
            <button
              type="button"
              onClick={handleLogout}
              disabled={isPending}
              className="text-muted-foreground hover:text-foreground disabled:opacity-50"
              aria-label="로그아웃"
            >
              <LogOut className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>
    </aside>
  );
}
