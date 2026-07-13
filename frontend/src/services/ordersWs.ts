/**
 * `/ws/orders` 백엔드 WebSocket 을 브라우저 탭당 1개 유지하는 싱글톤 매니저.
 *
 * 시세 WS 와 차이:
 * - subscribe 프로토콜 없음 — 서버가 JWT 로 memberId 를 알아내 본인 주문만 보냄.
 * - listener 는 심볼별이 아니라 전역. 어떤 컴포넌트든 붙으면 모든 내 주문을 받는다.
 * - 리스너가 하나라도 있으면 커넥션 유지, 0 개가 되면 idle close.
 * - 인증은 handshake 시 자동 전송되는 쿠키에 의존 (JwtAuthenticationFilter 가 검증).
 */

export type OrderEvent = {
  type: "order";
  orderId: string;
  symbol: string;
  stockName: string;
  side: "BUY" | "SELL";
  orderType: "MARKET" | "LIMIT";
  quantity: number;
  price: number;
  totalAmount: number;
  realizedProfitLoss: number | null;
  status: "PENDING" | "FILLED" | "CANCELED" | "REJECTED";
  at: string;
};

export type OrderEventListener = (event: OrderEvent) => void;

function resolveWsUrl(): string {
  const explicit = process.env.NEXT_PUBLIC_ORDERS_WS_URL;
  if (explicit) return explicit;
  const api = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return api.replace(/^http/, "ws") + "/ws/orders";
}

const WS_URL = resolveWsUrl();
const IDLE_CLOSE_MS = 5000;
const RECONNECT_INITIAL_MS = 1500;
const RECONNECT_MAX_MS = 30_000;

const listeners = new Set<OrderEventListener>();
let ws: WebSocket | null = null;
let connecting = false;
let reconnectDelay = RECONNECT_INITIAL_MS;
let idleTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

function scheduleIdleClose() {
  if (idleTimer) clearTimeout(idleTimer);
  idleTimer = setTimeout(() => {
    if (listeners.size === 0 && ws) {
      ws.close(1000, "idle");
      ws = null;
    }
  }, IDLE_CLOSE_MS);
}

function cancelIdleClose() {
  if (idleTimer) {
    clearTimeout(idleTimer);
    idleTimer = null;
  }
}

function ensureConnected() {
  if (typeof window === "undefined") return;
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return;
  if (connecting) return;
  connect();
}

function connect() {
  connecting = true;
  cancelIdleClose();
  let socket: WebSocket;
  try {
    socket = new WebSocket(WS_URL);
  } catch (err) {
    console.warn("[ordersWs] construct failed", err);
    connecting = false;
    scheduleReconnect();
    return;
  }
  ws = socket;
  socket.addEventListener("open", () => {
    connecting = false;
    reconnectDelay = RECONNECT_INITIAL_MS;
  });
  socket.addEventListener("message", (event) => {
    try {
      const data = JSON.parse(event.data) as OrderEvent | { type: string };
      if (data.type !== "order") return;
      const orderEvent = data as OrderEvent;
      listeners.forEach((fn) => {
        try {
          fn(orderEvent);
        } catch (e) {
          console.debug("[ordersWs] listener threw", e);
        }
      });
    } catch (e) {
      console.debug("[ordersWs] parse failed", e);
    }
  });
  socket.addEventListener("close", () => {
    connecting = false;
    ws = null;
    if (listeners.size > 0) scheduleReconnect();
  });
  socket.addEventListener("error", () => {
    console.debug("[ordersWs] socket error");
  });
}

function scheduleReconnect() {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  const delay = reconnectDelay;
  reconnectDelay = Math.min(RECONNECT_MAX_MS, reconnectDelay * 2);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    ensureConnected();
  }, delay);
}

/** 주문 이벤트 리스너 등록. 반환 함수 호출로 해제. */
export function subscribeToOrders(listener: OrderEventListener): () => void {
  listeners.add(listener);
  ensureConnected();
  cancelIdleClose();

  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) scheduleIdleClose();
  };
}
