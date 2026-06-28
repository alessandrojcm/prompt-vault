import { listAdminUsersOptions } from '@prompt-vault/api-client';
import { Stack } from '@mantine/core';
import { useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';

import { UserManagementTable } from '../../../features/admin/user-management';
import { requireAdminUser } from '../../../features/auth/current-user';

export const Route = createFileRoute("/dashboard/admin/users")({
  beforeLoad: requireAdminUser,
  component: UserManagementPage,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listAdminUsersOptions({ query: { role: "USER" } }));
  },
});

function UserManagementPage() {
  const users = useSuspenseQuery(listAdminUsersOptions({ query: { role: "USER" } }));

  return (
    <Stack gap="md">
      <UserManagementTable users={users.data ?? []} />
    </Stack>
  );
}
