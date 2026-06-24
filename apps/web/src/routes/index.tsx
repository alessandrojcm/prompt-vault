import { getCurrentUserOptions } from "@prompt-vault/api-client";
import { Card } from "@mantine/core";
import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  beforeLoad: async ({ context, abortController }) => {
    try {
      return await context.queryClient.ensureQueryData(
        getCurrentUserOptions({
          signal: abortController.signal,
        }),
      );
    } catch {
      console.error("Error getting session, redirecting to login");
      throw redirect({ to: "/login" });
    }
  },
  component: HomePage,
});

function HomePage() {
  return (
    <Card
      aria-label="Authenticated landing page"
      mih={240}
      mx="auto"
      radius="md"
      shadow="sm"
      withBorder
    />
  );
}
