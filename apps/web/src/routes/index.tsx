import { getCurrentUser } from "@prompt-vault/api-client";
import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  loader: async () => {
    // TODO: USE TANSTACK QUERY
    const result = await getCurrentUser();
    const status = result.response?.status;

    if (status === 401) {
      throw redirect({ to: "/signup" });
    }

    if (status !== 204) {
      throw new Error(`Unexpected current user response: ${status ?? "unknown"}`);
    }
  },
  component: HomePage,
});

function HomePage() {
  return (
    <main
      style={{ fontFamily: "sans-serif", margin: "0 auto", maxWidth: 720, padding: "4rem 1.5rem" }}
    >
      <p style={{ letterSpacing: "0.08em", textTransform: "uppercase" }}>Prompt Vault</p>
      <h1>Prompt Vault</h1>
      <p>Authenticated session detected.</p>
    </main>
  );
}
