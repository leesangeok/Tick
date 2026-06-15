"use client";

import { create } from "zustand";
import {
  addWatchlist,
  fetchWatchlist,
  removeWatchlist,
} from "@/services/watchlistService";

type State = {
  symbols: string[];
  isHydrated: boolean;
  hydrate: () => Promise<void>;
  add: (symbol: string) => Promise<void>;
  remove: (symbol: string) => Promise<void>;
  toggle: (symbol: string) => Promise<void>;
  has: (symbol: string) => boolean;
};

// 서버가 진실 source. 첫 mount 시 hydrate() 1 회 호출 → symbols 채움.
// add/remove 는 optimistic update + 실패 시 rollback.
export const useWatchlistStore = create<State>()((set, get) => ({
  symbols: [],
  isHydrated: false,

  hydrate: async () => {
    if (get().isHydrated) return;
    try {
      const symbols = await fetchWatchlist();
      set({ symbols, isHydrated: true });
    } catch {
      // 비로그인 (401) 등은 빈 배열로. isHydrated 는 false 유지 → 로그인 후 재시도.
      set({ symbols: [] });
    }
  },

  add: async (symbol) => {
    if (get().symbols.includes(symbol)) return;
    const prev = get().symbols;
    set({ symbols: [...prev, symbol] });
    try {
      await addWatchlist(symbol);
    } catch (e) {
      set({ symbols: prev });
      throw e;
    }
  },

  remove: async (symbol) => {
    const prev = get().symbols;
    if (!prev.includes(symbol)) return;
    set({ symbols: prev.filter((s) => s !== symbol) });
    try {
      await removeWatchlist(symbol);
    } catch (e) {
      set({ symbols: prev });
      throw e;
    }
  },

  toggle: async (symbol) => {
    if (get().symbols.includes(symbol)) await get().remove(symbol);
    else await get().add(symbol);
  },

  has: (symbol) => get().symbols.includes(symbol),
}));
