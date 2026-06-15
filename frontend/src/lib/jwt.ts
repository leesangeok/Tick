// JWT decode only (signature 검증은 백엔드 책임).
// edge runtime 호환 — base64url → JSON.

export type JwtPayload = {
  sub?: string;
  exp?: number; // unix seconds
  iat?: number;
  [k: string]: unknown;
};

export function decodeJwt(token: string): JwtPayload | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const payload = parts[1];
  if (!payload) return null;
  try {
    const padded = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(padded);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/**
 * access token 이 유효한지 (만료 30 초 전부터는 갱신 필요로 판단).
 */
export function isAccessFresh(token: string | undefined, bufferSec = 30): boolean {
  if (!token) return false;
  const payload = decodeJwt(token);
  if (!payload?.exp) return false;
  return payload.exp * 1000 > Date.now() + bufferSec * 1000;
}
