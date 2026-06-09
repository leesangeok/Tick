"use client";

import { useSyncExternalStore } from "react";
import { useTheme } from "next-themes";
import { Bell, Moon, Search, Sun } from "lucide-react";

function useIsClient(): boolean {
  return useSyncExternalStore(
    () => () => {},
    () => true,
    () => false,
  );
}

export function TopNavigation() {
  const { resolvedTheme, setTheme } = useTheme();
  const isClient = useIsClient();
  const isDark = isClient && resolvedTheme === "dark";

  return (
    <header className="sticky top-0 z-40 flex h-16 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur md:px-8">
      <div className="hidden flex-1 items-center gap-2 rounded-md border border-border bg-card px-3 py-2 md:flex md:max-w-md">
        <Search className="h-4 w-4 text-muted-foreground" />
        <input
          type="search"
          placeholder="종목 검색 (단축키 /)"
          className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
      </div>
      <div className="flex-1 text-lg font-bold md:hidden">
        Tick<span className="text-gain">.</span>
      </div>
      <button
        type="button"
        onClick={() => setTheme(isDark ? "light" : "dark")}
        className="flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground"
        aria-label="테마 전환"
      >
        {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      </button>
      <button
        type="button"
        className="flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground"
        aria-label="알림"
      >
        <Bell className="h-4 w-4" />
      </button>
    </header>
  );
}
