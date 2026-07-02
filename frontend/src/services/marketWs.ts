/**
 * `/ws/market` 백엔드 WebSocket 을 브라우저 탭 하나당 1개 유지하는 싱글톤 매니저.
 *
 * 왜 싱글톤인가:
 * - 여러 컴포넌트 (상세 페이지 가격 헤더 / 대시보드 위젯 등) 가 각자 useMarketTick 을 쓰더라도
 *   물리 커넥션은 1개. 백엔드 세션 리소스 절약 + 심볼별 refcount 는 백엔드가 이미 관리.
 *
 * 동작:
 * - 최초 subscribe 시 lazy 하게 커넥션 open + 백엔드로 {action:"subscribe",symbol} 전송
 * - 마지막 리스너 해제 시 {action:"unsubscribe",symbol} 만 보냄 (커넥션은 idle 유지 → 짧은 재구독 대응)
 * - 5초간 리스너 0 상태 지속 시 커넥션 close
 * - 커넥션 끊기면 1.5초 backoff (최대 30초) 로 재접속 후 활성 심볼 전부 재구독
 */

type MarketTick = {
  type: "tick";
  symbol: string;
  price: number;
  changeAmount: number;
  changeRate: number;
  volume: number;
  at: string;
};

type ServerMessage =
  | MarketTick
  | { type: "ack"; action: "subscribe" | "unsubscribe"; symbol: string }
  | { type: "error"; message: string };

export type TickListener = (tick: MarketTick) => void;

function resolveWsUrl(): string {
  const explicit = process.env.NEXT_PUBLIC_WS_URL;
  if (explicit) return explicit;
  const api = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return api.replace(/^http/, "ws") + "/ws/market";
}

const WS_URL = resolveWsUrl();
const IDLE_CLOSE_MS = 5000;
const RECONNECT_INITIAL_MS = 1500;
const RECONNECT_MAX_MS = 30_000;

// symbol → listeners
const listeners = new Map<string, Set<TickListener>>();
let ws: WebSocket | null = null;
let connecting = false;
let reconnectDelay = RECONNECT_INITIAL_MS;
let idleTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

function activeSymbols(): string[] {
  return Array.from(listeners.entries())
    .filter(([, set]) => set.size > 0)
    .map(([symbol]) => symbol);
}

function totalListeners(): number {
  let n = 0;
  listeners.forEach((set) => {
    n += set.size;
  });
  return n;
}

function sendJson(payload: unknown) {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload));
  }
}

function scheduleIdleClose() {
  if (idleTimer) clearTimeout(idleTimer);
  idleTimer = setTimeout(() => {
    if (totalListeners() === 0 && ws) {
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
  if (typeof window === "undefined") return; // SSR 안전 가드
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
    console.warn("[marketWs] construct failed", err);
    connecting = false;
    scheduleReconnect();
    return;
  }
  ws = socket;
  socket.addEventListener("open", () => {
    connecting = false;
    reconnectDelay = RECONNECT_INITIAL_MS;
    // 재접속 시 활성 심볼 전부 재구독
    activeSymbols().forEach((symbol) => sendJson({ action: "subscribe", symbol }));
  });
  socket.addEventListener("message", (event) => {
    try {
      const data = JSON.parse(event.data) as ServerMessage;
      if (data.type === "tick") {
        const set = listeners.get(data.symbol);
        if (!set) return;
        set.forEach((fn) => {
          try {
            fn(data);
          } catch (e) {
            console.debug("[marketWs] listener threw", e);
          }
        });
      }
      // ack / error 는 현재 조용히 무시. debug 필요 시 여기 로깅.
    } catch (e) {
      console.debug("[marketWs] parse failed", e);
    }
  });
  socket.addEventListener("close", () => {
    connecting = false;
    ws = null;
    // 아직 붙고 싶은 심볼이 있으면 재접속
    if (activeSymbols().length > 0) scheduleReconnect();
  });
  socket.addEventListener("error", () => {
    // close 가 뒤따라 오므로 여기선 로깅만
    console.debug("[marketWs] socket error");
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

/** 특정 심볼에 리스너 붙임. 반환 함수 호출로 해제. */
export function subscribeToTicks(symbol: string, listener: TickListener): () => void {
  const set = listeners.get(symbol) ?? new Set<TickListener>();
  const wasEmpty = set.size === 0;
  set.add(listener);
  listeners.set(symbol, set);

  ensureConnected();
  if (wasEmpty) sendJson({ action: "subscribe", symbol });
  cancelIdleClose();

  return () => {
    const current = listeners.get(symbol);
    if (!current) return;
    current.delete(listener);
    if (current.size === 0) {
      listeners.delete(symbol);
      sendJson({ action: "unsubscribe", symbol });
    }
    if (totalListeners() === 0) scheduleIdleClose();
  };
}

export type { MarketTick };
