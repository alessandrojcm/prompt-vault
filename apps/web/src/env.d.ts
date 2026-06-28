/// <reference types="vite/client" />

interface ImportMetaEnv {
  // Client-side environment variables
  readonly VITE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Server-side environment variables
declare global {
  namespace NodeJS {
    interface ProcessEnv {
      readonly PROMPT_VAULT_API_BASE_URL: string;
      readonly OPENROUTER_API_KEY: string;
    }
  }
}

export {};
