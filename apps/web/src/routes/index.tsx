import {
  getCurrentUserOptions,
  getCurrentUserQueryKey,
  logoutMutation,
} from "@prompt-vault/api-client";
import { Alert, Button, Card, Stack, Text, Title } from "@mantine/core";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";

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
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const logout = useMutation(logoutMutation());

  async function handleLogout() {
    try {
      await logout.mutateAsync({});
      queryClient.removeQueries({ queryKey: getCurrentUserQueryKey() });
      await navigate({ to: "/login" });
    } catch {
      // The mutation state renders a retryable error message.
    }
  }

  return (
    <Card
      aria-label="Authenticated landing page"
      mih={240}
      mx="auto"
      padding="xl"
      radius="md"
      shadow="sm"
      withBorder
    >
      <Stack align="flex-start" gap="md">
        <div>
          <Text c="dimmed" fw={700} size="xs" tt="uppercase">
            Signed in
          </Text>
          <Title order={2}>Your prompt vault is ready.</Title>
        </div>
        <Text c="dimmed">Log out when you're finished using this session.</Text>
        {logout.isError ? (
          <Alert color="red" title="Could not log out" variant="light">
            Try again before closing this browser tab.
          </Alert>
        ) : null}
        <Button loading={logout.isPending} onClick={handleLogout}>
          Log out
        </Button>
      </Stack>
    </Card>
  );
}
