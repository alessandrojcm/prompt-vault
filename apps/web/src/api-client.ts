import { client } from '@prompt-vault/api-client';
import { createIsomorphicFn } from '@tanstack/react-start';
import { getRequest } from '@tanstack/react-start/server';

export function configureApiClient() {
  client.setConfig({
    baseUrl: getApiBaseUrl(),
    credentials: "include",
    headers: getApiHeaders(),
  });
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
