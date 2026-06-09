import type { Order } from "@/types/order";
import { apiFetch } from "./apiFetch";

export async function fetchOrders(): Promise<Order[]> {
  const res = await apiFetch("/api/orders");
  if (!res.ok) throw new Error(`Failed to fetch orders: ${res.status}`);
  return res.json();
}
