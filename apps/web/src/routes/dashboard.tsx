import { getCurrentUserQueryKey, logoutMutation } from "@prompt-vault/api-client";
import { Alert, Button, Card, Stack, Text, Title } from "@mantine/core";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";

import { requireCurrentUser } from "../features/auth/current-user";

export const Route = createFileRoute("/dashboard")({
  beforeLoad: requireCurrentUser,
  component: DashboardPage,
});

function DashboardPage() {
  const navigate = useNavigate();
  const router = useRouter();
  const queryClient = useQueryClient();
  const logout = useMutation(logoutMutation());

  async function handleLogout() {
    logout.mutate(
      {},
      {
        onSuccess: async () => {
          queryClient.removeQueries({ queryKey: getCurrentUserQueryKey() });
          await router.invalidate();
          await navigate({ to: "/login" });
        },
      },
    );
  }

  return (
    <Card
      aria-label="Authenticated dashboard"
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
            Dashboard
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
