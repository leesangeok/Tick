import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

type Handler = (event: unknown) => void;

class MockWebSocket {
  static instances: MockWebSocket[] = [];

  readonly url: string;
  readyState = 0;
  sent: string[] = [];
  private handlers: Record<string, Handler[]> = {};

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  addEventListener(type: string, cb: Handler) {
    (this.handlers[type] ??= []).push(cb);
  }

  send(payload: string) {
    this.sent.push(payload);
  }

  close(code?: number) {
    this.readyState = 3;
    this.emit("close", { code });
  }

  emit(type: string, event: unknown) {
    (this.handlers[type] ?? []).forEach((cb) => cb(event));
  }

  simulateOpen() {
    this.readyState = 1;
    this.emit("open", {});
  }

  simulateMessage(data: unknown) {
    this.emit("message", { data: JSON.stringify(data) });
  }
}
(MockWebSocket as unknown as { CONNECTING: number }).CONNECTING = 0;
(MockWebSocket as unknown as { OPEN: number }).OPEN = 1;
(MockWebSocket as unknown as { CLOSING: number }).CLOSING = 2;
(MockWebSocket as unknown as { CLOSED: number }).CLOSED = 3;

describe("ordersWs", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.resetModules();
    MockWebSocket.instances = [];
    (globalThis as { WebSocket?: unknown }).WebSocket = MockWebSocket;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("subscribe 시 커넥션이 열리고 subscribe/unsubscribe 프로토콜은 아무 것도 보내지 않는다", async () => {
    const { subscribeToOrders } = await import("./ordersWs");
    subscribeToOrders(() => {});
    expect(MockWebSocket.instances).toHaveLength(1);
    MockWebSocket.instances[0].simulateOpen();
    // 시세 WS 와 달리 프론트 → 백엔드 protocol 없음 (서버가 memberId 로 라우팅).
    expect(MockWebSocket.instances[0].sent).toEqual([]);
  });

  it("order 이벤트가 오면 모든 리스너에게 전달한다", async () => {
    const { subscribeToOrders } = await import("./ordersWs");
    const a = vi.fn();
    const b = vi.fn();
    subscribeToOrders(a);
    subscribeToOrders(b);
    MockWebSocket.instances[0].simulateOpen();

    const event = {
      type: "order",
      orderId: "ord_1",
      symbol: "005930",
      stockName: "삼성전자",
      side: "BUY",
      orderType: "MARKET",
      quantity: 10,
      price: 71000,
      totalAmount: 710000,
      realizedProfitLoss: null,
      status: "FILLED",
      at: "2026-07-07T00:00:00Z",
    };
    MockWebSocket.instances[0].simulateMessage(event);

    expect(a).toHaveBeenCalledWith(event);
    expect(b).toHaveBeenCalledWith(event);
  });

  it("order 이외 type 은 무시한다", async () => {
    const { subscribeToOrders } = await import("./ordersWs");
    const listener = vi.fn();
    subscribeToOrders(listener);
    MockWebSocket.instances[0].simulateOpen();

    MockWebSocket.instances[0].simulateMessage({ type: "heartbeat" });
    expect(listener).not.toHaveBeenCalled();
  });

  it("모든 리스너 해제 + idle 경과 시 커넥션이 닫힌다", async () => {
    const { subscribeToOrders } = await import("./ordersWs");
    const unsub = subscribeToOrders(() => {});
    MockWebSocket.instances[0].simulateOpen();

    unsub();
    vi.advanceTimersByTime(6000);
    expect(MockWebSocket.instances[0].readyState).toBe(3);
  });
});
