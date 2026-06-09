"use client";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type MeResponse = {
  id: number;
  nickname: string | null;
  email: string | null;
};

export type AccountResponse = {
  id: number;
  cash: number;
  totalDeposits: number;
  realizedProfitLoss: number;
};

export async function depositToAccount(amount: number): Promise<AccountResponse> {
  const res = await fetch(`${API_URL}/api/account/deposit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
    cache: "no-store",
    credentials: "include",
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`Deposit failed (${res.status}): ${body}`);
  }
  return res.json();
}

export async function fetchMeOnClient(): Promise<MeResponse | null> {
  const res = await fetch(`${API_URL}/api/auth/me`, {
    credentials: "include",
    cache: "no-store",
  });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error(`Failed to fetch me: ${res.status}`);
  return res.json();
}

export async function logout(): Promise<void> {
  await fetch(`${API_URL}/api/auth/logout`, {
    method: "POST",
    cache: "no-store",
    credentials: "include",
  });
}
