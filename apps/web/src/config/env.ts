// src/config/env.ts
import * as v from "valibot";

const envSchema = v.object({
  PROMPT_VAULT_API_BASE_URL: v.optional(v.pipe(v.string(), v.url()), "http://localhost:8080"),
});

const clientEnvSchema = v.object({
  VITE_API_URL: v.optional(v.pipe(v.string(), v.url()), "http://localhost:8080"),
  OPENROUTER_API_KEY: v.optional(v.pipe(v.string())),
});

export const serverEnv = v.parse(envSchema, process.env);

export const clientEnv = v.parse(clientEnvSchema, import.meta.env);
