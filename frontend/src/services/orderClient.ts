"use client";

import type { CreateOrderRequest, CreateOrderResponse } from "@/types/order";
import type { ApiResponse } from "./apiFetch";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class OrderApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  constructor(status: number, message: string, code: string | null) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function postOrder(
  side: "buy" | "sell",
  body: CreateOrderRequest,
): Promise<CreateOrderResponse> {
  const res = await fetch(`${API_URL}/api/v1/orders/${side}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    credentials: "include",
    cache: "no-store",
  });
  let parsed: ApiResponse<CreateOrderResponse> | null = null;
  try {
    parsed = (await res.json()) as ApiResponse<CreateOrderResponse>;
  } catch {
    // body가 JSON 이 아닐 수 있음 (e.g. 401 빈 본문)
  }
  if (!res.ok || !parsed?.success) {
    throw new OrderApiError(
      res.status,
      parsed?.message ?? `주문 실패 (HTTP ${res.status})`,
      parsed?.code ?? null,
    );
  }
  return parsed.data as CreateOrderResponse;
}

export const createBuyOrder = (body: CreateOrderRequest) => postOrder("buy", body);
export const createSellOrder = (body: CreateOrderRequest) => postOrder("sell", body);
