import type { Portfolio } from "@/types/portfolio";
import { apiFetch } from "./apiFetch";

export async function fetchPortfolio(): Promise<Portfolio> {
  const res = await apiFetch("/api/portfolio");
  if (!res.ok) throw new Error(`Failed to fetch portfolio: ${res.status}`);
  return res.json();
}
