import { getCurrentUserOptions } from "@prompt-vault/api-client";
import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  loader: async ({ context }) => {
    try {
      return await context.queryClient.ensureQueryData(getCurrentUserOptions());
    } catch (e) {
      console.error("Error getting session, redirecting to signin");
      throw redirect({ to: "/signup" });
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
