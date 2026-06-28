import { useMemo } from "react";

import {
  type AccountStatus,
  listAdminUsersQueryKey,
  type Options,
  type UpdateAdminUserStatusData,
  updateAdminUserStatusMutation,
  type UserSummary,
} from "@prompt-vault/api-client";
import { Badge, Button, Card, Popover, Stack, Table, Text, Title } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { MutationFunction, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  useReactTable,
} from "@tanstack/react-table";

const columnHelper = createColumnHelper<UserSummary>();

type UpdateUserStatus = MutationFunction<UserSummary, Options<UpdateAdminUserStatusData>>;

export function accountStatusLabel(accountStatus: AccountStatus) {
  return accountStatus === "ENABLED" ? "Enabled" : "Disabled";
}

export function UserManagementTable({
  updateUserStatus,
  users,
}: {
  updateUserStatus?: UpdateUserStatus;
  users: Array<UserSummary>;
}) {
  const queryClient = useQueryClient();
  const updateStatusMutation = useMutation({
    ...updateAdminUserStatusMutation(),
    ...(updateUserStatus ? { mutationFn: updateUserStatus } : {}),
    onSuccess: (updatedUser) => {
      queryClient.setQueryData(
        listAdminUsersQueryKey({ query: { role: "USER" } }),
        (currentUsers: Array<UserSummary> | undefined) =>
          currentUsers?.map((row) => (row.id === updatedUser.id ? updatedUser : row)),
      );
      notifications.show({
        color: "green",
        message: `${updatedUser.username} is now ${accountStatusLabel(updatedUser.accountStatus).toLowerCase()}.`,
        title: "Account status updated",
      });
    },
    onError: (_error, variables) => {
      const user = users.find((user) => user.id === variables.path.userId);
      notifications.show({
        color: "red",
        message: `Could not update ${user?.username ?? "the user"}'s account status.`,
        title: "Account status update failed",
      });
    },
  });

  async function setUserStatus(user: UserSummary, accountStatus: AccountStatus) {
    updateStatusMutation.mutate({
      body: { accountStatus },
      path: { userId: user.id },
    });
  }

  const userManagementColumns = useMemo(
    () => [
      columnHelper.accessor("username", {
        cell: (info) => info.getValue(),
        header: "Username",
      }),
      columnHelper.accessor("emailAddress", {
        cell: (info) => info.getValue(),
        header: "Email address",
      }),
      columnHelper.accessor("accountStatus", {
        cell: (info) => (
          <Badge color={info.getValue() === "ENABLED" ? "green" : "gray"} variant="light">
            {accountStatusLabel(info.getValue())}
          </Badge>
        ),
        header: "Account status",
      }),
      columnHelper.display({
        cell: ({ row }) => (
          <AccountStatusAction
            isUpdating={
              updateStatusMutation.isPending &&
              updateStatusMutation.variables.path.userId === row.original.id
            }
            onUpdate={(accountStatus) => void setUserStatus(row.original, accountStatus)}
            user={row.original}
          />
        ),
        header: "Actions",
        id: "actions",
      }),
    ],
    [updateStatusMutation.isPending, updateStatusMutation.variables],
  );

  const table = useReactTable({
    columns: userManagementColumns,
    data: users,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
  });

  return (
    <Card aria-label="User Management" padding="xl" radius="md" shadow="sm" withBorder>
      <Stack gap="lg">
        <div>
          <Text c="dimmed" fw={700} size="xs" tt="uppercase">
            Admin
          </Text>
          <Title order={2}>User Management</Title>
        </div>
        <Table.ScrollContainer minWidth={680}>
          <Table verticalSpacing="sm">
            <Table.Thead>
              {table.getHeaderGroups().map((headerGroup) => (
                <Table.Tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <Table.Th key={header.id}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                    </Table.Th>
                  ))}
                </Table.Tr>
              ))}
            </Table.Thead>
            <Table.Tbody>
              {table.getRowModel().rows.map((row) => (
                <Table.Tr key={row.id}>
                  {row.getVisibleCells().map((cell) => (
                    <Table.Td key={cell.id}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </Table.Td>
                  ))}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Stack>
    </Card>
  );
}

function AccountStatusAction({
  isUpdating,
  onUpdate,
  user,
}: {
  isUpdating: boolean;
  onUpdate: (accountStatus: AccountStatus) => void;
  user: UserSummary;
}) {
  if (user.accountStatus === "DISABLED") {
    return (
      <Button loading={isUpdating} onClick={() => onUpdate("ENABLED")} size="xs" variant="light">
        Enable
      </Button>
    );
  }

  return (
    <Popover position="bottom-end" shadow="md" withArrow>
      <Popover.Target>
        <Button color="red" loading={isUpdating} size="xs" variant="light">
          Disable
        </Button>
      </Popover.Target>
      <Popover.Dropdown>
        <Stack gap="xs">
          <Text size="sm">Disable {user.username}?</Text>
          <Button color="red" onClick={() => onUpdate("DISABLED")} size="xs">
            Confirm disable
          </Button>
        </Stack>
      </Popover.Dropdown>
    </Popover>
  );
}
