import type { Transaction } from "@/types/account";
import { apiFetch } from "./apiFetch";

export async function fetchTransactions(): Promise<Transaction[]> {
  const res = await apiFetch("/api/transactions");
  if (!res.ok) throw new Error(`Failed to fetch transactions: ${res.status}`);
  return res.json();
}
