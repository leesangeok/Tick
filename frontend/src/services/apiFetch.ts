import { cookies } from "next/headers";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * 백엔드 공통 응답 형태. 모든 /api/v1/* 응답이 이 래퍼를 사용한다.
 * 실패 시 data 는 null, message + code 채워짐.
 */
export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
  code: string | null;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  constructor(status: number, message: string, code: string | null) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

/**
 * 서버 컴포넌트에서 백엔드 호출 시 사용.
 * 브라우저의 tick_at 쿠키를 backend 로 forward 한다.
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map((c) => `${c.name}=${c.value}`)
    .join("; ");

  const headers = new Headers(init.headers);
  if (cookieHeader) headers.set("Cookie", cookieHeader);

  return fetch(`${API_URL}${path}`, {
    cache: "no-store",
    ...init,
    headers,
  });
}

/**
 * ApiResponse 래퍼를 벗기고 data 만 반환한다.
 * - res.ok 가 false 이거나 success=false 면 ApiError throw.
 * - 사용처에서 404 같은 케이스를 null 로 처리하고 싶다면 호출 전 res.status 체크하거나
 *   ApiError 를 catch 해서 status 분기.
 */
export async function unwrapApi<T>(res: Response): Promise<T> {
  let body: ApiResponse<T> | null = null;
  try {
    body = (await res.json()) as ApiResponse<T>;
  } catch {
    // body 가 JSON 이 아닌 경우 (e.g. 401 with empty body)
  }
  if (!res.ok || !body?.success) {
    const message = body?.message ?? `HTTP ${res.status}`;
    const code = body?.code ?? null;
    throw new ApiError(res.status, message, code);
  }
  return body.data as T;
}

export { API_URL };
