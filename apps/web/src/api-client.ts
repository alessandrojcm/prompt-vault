import { client } from "@prompt-vault/api-client";
import { createIsomorphicFn } from "@tanstack/react-start";
import { getRequest } from "@tanstack/react-start/server";

// SPA CSRF contract: the API renders the token in the `XSRF-TOKEN` cookie on any
// response (including unauthenticated ones) and requires it back in the
// `X-XSRF-TOKEN` header on state-changing requests. Guarded for SSR, where there
// is no `document` and the forwarded Cookie header carries the session instead.
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

function readCsrfCookie(): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${CSRF_COOKIE_NAME}=([^\\s;]*)`));
  return match === null ? null : decodeURIComponent(match[1]);
}

function getApiBaseUrl() {
  if (import.meta.env.SSR) {
    return process.env.PROMPT_VAULT_API_BASE_URL;
  }

  return import.meta.env.VITE_API_URL;
}

const getApiHeaders = createIsomorphicFn()
  .client(() => ({}))
  .server(() => {
    const cookie = getRequest().headers.get("cookie");

    return cookie === null ? {} : { cookie };
  });

export function configureApiClient() {
  client.setConfig({
    baseUrl: getApiBaseUrl(),
    credentials: "include",
    headers: getApiHeaders(),
  });

  client.interceptors.request.use((request) => {
    if (typeof document !== "undefined" && request.method.toUpperCase() !== "GET") {
      const csrfToken = readCsrfCookie();
      if (csrfToken !== null) {
        request.headers.set(CSRF_HEADER_NAME, csrfToken);
      }
    }
    return request;
  });
}
