/**
 * jsdom 환경에서 부족한 브라우저 API polyfill.
 * marketWs / ordersWs 가 WebSocket 을 필드로 잡고 있어 최소 mock 이 필요.
 */

// jsdom 에는 WebSocket 이 없다. 각 테스트가 필요에 따라 자기만의 mock 을 넣으므로 여기선
// readyState 상수만 노출 (production 코드가 WebSocket.OPEN 등을 참조).
class WebSocketStub {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSING = 2;
  static readonly CLOSED = 3;
}
if (typeof (globalThis as { WebSocket?: unknown }).WebSocket === "undefined") {
  (globalThis as { WebSocket?: unknown }).WebSocket = WebSocketStub;
}
