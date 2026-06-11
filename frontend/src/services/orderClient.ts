"use client";

import type { CreateOrderRequest, CreateOrderResponse } from "@/types/order";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type ApiErrorBody = {
  success: false;
  code: string;
  message: string;
};

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
  const res = await fetch(`${API_URL}/api/orders/${side}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    credentials: "include",
    cache: "no-store",
  });
  if (!res.ok) {
    let code: string | null = null;
    let message = `주문 실패 (HTTP ${res.status})`;
    try {
      const parsed = (await res.json()) as ApiErrorBody;
      code = parsed.code ?? null;
      if (parsed.message) message = parsed.message;
    } catch {
      // body가 JSON 이 아닐 수 있음 (e.g. 401 빈 본문)
    }
    throw new OrderApiError(res.status, message, code);
  }
  return res.json();
}

export const createBuyOrder = (body: CreateOrderRequest) => postOrder("buy", body);
export const createSellOrder = (body: CreateOrderRequest) => postOrder("sell", body);
