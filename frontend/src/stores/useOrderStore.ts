"use client";

import { create } from "zustand";

type Side = "BUY" | "SELL";

type State = {
  side: Side;
  setSide: (side: Side) => void;
  toggleSide: () => void;
};

export const useOrderStore = create<State>((set) => ({
  side: "BUY",
  setSide: (side) => set({ side }),
  toggleSide: () => set((s) => ({ side: s.side === "BUY" ? "SELL" : "BUY" })),
}));
