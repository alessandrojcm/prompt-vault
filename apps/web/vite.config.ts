import { playwright } from "@vitest/browser-playwright";
import { defineConfig } from "vitest/config";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import { devtools } from "@tanstack/devtools-vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  resolve: {
    dedupe: ["react", "react-dom"],
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/v3/api-docs": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/swagger-ui": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/swagger-ui.html": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  test: {
    browser: {
      enabled: true,
      headless: true,
      instances: [{ browser: "chromium" }],
      provider: playwright(),
    },
    globals: true,
  },
  plugins: [devtools(), tanstackStart(), react()],
});
