import { ApiError, apiFetch, unwrapApi } from "./apiFetch";

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
  const res = await apiFetch("/api/v1/account");
  return unwrapApi<AccountResponse>(res);
}

export async function fetchMe(): Promise<MeResponse | null> {
  const res = await apiFetch("/api/v1/auth/me");
  try {
    return await unwrapApi<MeResponse>(res);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return null;
    throw err;
  }
}
