import { cookies } from "next/headers";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

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

export { API_URL };
