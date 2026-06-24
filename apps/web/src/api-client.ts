import { client } from "@prompt-vault/api-client";

export function configureApiClient() {
  client.setConfig({
    baseUrl: getApiBaseUrl(),
    credentials: "same-origin",
  });
}

function getApiBaseUrl() {
  if (import.meta.env.SSR) {
    return process.env.PROMPT_VAULT_API_BASE_URL;
  }

  return import.meta.env.VITE_API_URL;
}
