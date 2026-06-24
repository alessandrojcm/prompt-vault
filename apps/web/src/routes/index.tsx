import { getCurrentUserOptions } from "@prompt-vault/api-client";
import { Card, Text, Title } from "@mantine/core";
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
      console.error("Error getting session, redirecting to signin");
      throw redirect({ to: "/signup" });
    }
  },
  component: HomePage,
});

function HomePage() {
  return (
    <Card maw={520} mx="auto" padding="xl" radius="md" shadow="sm" withBorder>
      <Text c="dimmed" fw={700} size="xs" tt="uppercase">
        Prompt Vault
      </Text>
      <Title order={2}>Prompt Vault</Title>
      <Text mt="md">Authenticated session detected.</Text>
    </Card>
  );
}
