import { apiFetch } from "./apiFetch";

export type AccountResponse = {
  id: number;
  cash: number;
  totalDeposits: number;
  realizedProfitLoss: number;
};

export type MeResponse = {
  id: number;
  nickname: string | null;
  email: string | null;
};

export async function fetchAccount(): Promise<AccountResponse> {
  const res = await apiFetch("/api/account");
  if (!res.ok) throw new Error(`Failed to fetch account: ${res.status}`);
  return res.json();
}

export async function fetchMe(): Promise<MeResponse | null> {
  const res = await apiFetch("/api/auth/me");
  if (res.status === 401) return null;
  if (!res.ok) throw new Error(`Failed to fetch me: ${res.status}`);
  return res.json();
}
