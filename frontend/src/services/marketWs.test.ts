import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/**
 * `marketWs` 싱글톤은 모듈 스코프에 상태를 저장하므로 각 테스트가 `vi.resetModules()` 로
 * 재로딩한다. WebSocket 은 인스턴스별 stub 을 window 에 심어서 open/message/close 를 수동 제어.
 */

type Handler = (event: unknown) => void;

class MockWebSocket {
  static instances: MockWebSocket[] = [];

  readonly url: string;
  readyState = 0; // CONNECTING
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

// production 코드가 참조하는 상수.
(MockWebSocket as unknown as { CONNECTING: number }).CONNECTING = 0;
(MockWebSocket as unknown as { OPEN: number }).OPEN = 1;
(MockWebSocket as unknown as { CLOSING: number }).CLOSING = 2;
(MockWebSocket as unknown as { CLOSED: number }).CLOSED = 3;

describe("marketWs", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.resetModules();
    MockWebSocket.instances = [];
    (globalThis as { WebSocket?: unknown }).WebSocket = MockWebSocket;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("첫 subscribe 시 커넥션을 열고 subscribe 메시지를 보낸다", async () => {
    const { subscribeToTicks } = await import("./marketWs");
    subscribeToTicks("005930", () => {});

    expect(MockWebSocket.instances).toHaveLength(1);
    const socket = MockWebSocket.instances[0];
    socket.simulateOpen();

    expect(socket.sent).toContain(JSON.stringify({ action: "subscribe", symbol: "005930" }));
  });

  it("tick 메시지가 오면 심볼이 일치하는 리스너에게 전달한다", async () => {
    const { subscribeToTicks } = await import("./marketWs");
    const listener = vi.fn();
    subscribeToTicks("005930", listener);
    MockWebSocket.instances[0].simulateOpen();

    MockWebSocket.instances[0].simulateMessage({
      type: "tick",
      symbol: "005930",
      price: 71000,
      changeAmount: -1500,
      changeRate: -2.07,
      volume: 123,
      at: "2026-07-07T00:00:00Z",
    });

    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener).toHaveBeenCalledWith(expect.objectContaining({ symbol: "005930", price: 71000 }));
  });

  it("다른 심볼의 tick 은 리스너에게 전달되지 않는다", async () => {
    const { subscribeToTicks } = await import("./marketWs");
    const listener = vi.fn();
    subscribeToTicks("005930", listener);
    MockWebSocket.instances[0].simulateOpen();

    MockWebSocket.instances[0].simulateMessage({
      type: "tick",
      symbol: "000660",
      price: 1,
      changeAmount: 0,
      changeRate: 0,
      volume: 0,
      at: "2026-07-07T00:00:00Z",
    });

    expect(listener).not.toHaveBeenCalled();
  });

  it("마지막 리스너 해제 시 unsubscribe 메시지 전송 + idle 지나면 커넥션 close", async () => {
    const { subscribeToTicks } = await import("./marketWs");
    const unsub = subscribeToTicks("005930", () => {});
    MockWebSocket.instances[0].simulateOpen();

    unsub();
    expect(MockWebSocket.instances[0].sent).toContain(
      JSON.stringify({ action: "unsubscribe", symbol: "005930" }),
    );

    // idle 5초 지나면 close 호출.
    vi.advanceTimersByTime(6000);
    expect(MockWebSocket.instances[0].readyState).toBe(3);
  });
});
