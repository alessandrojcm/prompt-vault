import { listAdminUsersOptions } from "@prompt-vault/api-client";
import { Alert, Loader, Stack } from "@mantine/core";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";

import { requireAdminUser } from "../../features/auth/current-user";
import { UserManagementTable } from "../../features/admin/user-management";

export const Route = createFileRoute("/admin/users")({
  beforeLoad: requireAdminUser,
  component: UserManagementPage,
});

function UserManagementPage() {
  const users = useQuery(listAdminUsersOptions({ query: { role: "USER" } }));

  if (users.isLoading) {
    return <Loader aria-label="Loading users" />;
  }

  if (users.isError) {
    return (
      <Alert color="red" title="Could not load users" variant="light">
        Try refreshing the page.
      </Alert>
    );
  }

  return (
    <Stack gap="md">
      <UserManagementTable users={users.data ?? []} />
    </Stack>
  );
}
