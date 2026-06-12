import type { Order } from "@/types/order";
import { apiFetch, unwrapApi } from "./apiFetch";

export async function fetchOrders(): Promise<Order[]> {
  const res = await apiFetch("/api/v1/orders");
  return unwrapApi<Order[]>(res);
}
