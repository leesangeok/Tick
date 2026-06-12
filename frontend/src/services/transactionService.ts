import type { Transaction } from "@/types/account";
import { apiFetch, unwrapApi } from "./apiFetch";

export async function fetchTransactions(): Promise<Transaction[]> {
  const res = await apiFetch("/api/v1/transactions");
  return unwrapApi<Transaction[]>(res);
}
