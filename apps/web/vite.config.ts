import { defineConfig } from "vite";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
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
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
  },
  plugins: [tanstackStart(), react()],
});
