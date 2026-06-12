"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

type State = {
  symbols: string[];
  add: (symbol: string) => void;
  remove: (symbol: string) => void;
  toggle: (symbol: string) => void;
  has: (symbol: string) => boolean;
};

export const useWatchlistStore = create<State>()(
  persist(
    (set, get) => ({
      symbols: [],
      add: (symbol) =>
        set((s) => (s.symbols.includes(symbol) ? s : { symbols: [...s.symbols, symbol] })),
      remove: (symbol) =>
        set((s) => ({ symbols: s.symbols.filter((it) => it !== symbol) })),
      toggle: (symbol) =>
        set((s) =>
          s.symbols.includes(symbol)
            ? { symbols: s.symbols.filter((it) => it !== symbol) }
            : { symbols: [...s.symbols, symbol] },
        ),
      has: (symbol) => get().symbols.includes(symbol),
    }),
    { name: "tick:watchlist" },
  ),
);
