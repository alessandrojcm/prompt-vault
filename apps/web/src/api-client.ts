import { client } from "@prompt-vault/api-client";

export function configureApiClient() {
  client.setConfig({
    baseUrl: getApiBaseUrl(),
    credentials: "same-origin",
  });
}

function getApiBaseUrl() {
  if (import.meta.env.SSR) {
    return import.meta.env.PROMPT_VAULT_API_BASE_URL ?? "http://localhost:8080/api";
  }

  return "/";
}
