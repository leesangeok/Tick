"use client";

import type { ApiResponse } from "./apiFetch";

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

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  constructor(status: number, message: string, code: string | null) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function unwrap<T>(res: Response, fallback: string): Promise<T> {
  let body: ApiResponse<T> | null = null;
  try {
    body = (await res.json()) as ApiResponse<T>;
  } catch {
    // body 가 JSON 이 아닐 수 있음
  }
  if (!res.ok || !body?.success) {
    throw new ApiError(
      res.status,
      body?.message ?? `${fallback} (HTTP ${res.status})`,
      body?.code ?? null,
    );
  }
  return body.data as T;
}

export async function depositToAccount(amount: number): Promise<AccountResponse> {
  const res = await fetch(`${API_URL}/api/v1/account/deposit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
    cache: "no-store",
    credentials: "include",
  });
  return unwrap<AccountResponse>(res, "Deposit failed");
}

export async function fetchMeOnClient(): Promise<MeResponse | null> {
  const res = await fetch(`${API_URL}/api/v1/auth/me`, {
    credentials: "include",
    cache: "no-store",
  });
  if (res.status === 401) return null;
  return unwrap<MeResponse>(res, "Failed to fetch me");
}

export async function logout(): Promise<void> {
  await fetch(`${API_URL}/api/v1/auth/logout`, {
    method: "POST",
    cache: "no-store",
    credentials: "include",
  });
}
