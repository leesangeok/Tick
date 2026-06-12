import type { Portfolio } from "@/types/portfolio";
import { apiFetch, unwrapApi } from "./apiFetch";

export async function fetchPortfolio(): Promise<Portfolio> {
  const res = await apiFetch("/api/v1/portfolio");
  return unwrapApi<Portfolio>(res);
}
