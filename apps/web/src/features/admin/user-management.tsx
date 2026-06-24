import type { AccountStatus, UserSummary } from "@prompt-vault/api-client";
import { Badge, Button, Card, Group, Stack, Table, Text, Title } from "@mantine/core";
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  useReactTable,
} from "@tanstack/react-table";

const columnHelper = createColumnHelper<UserSummary>();

const userManagementColumns = [
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
    cell: () => (
      <Group gap="xs">
        <Button disabled size="xs" variant="light">
          Manage
        </Button>
      </Group>
    ),
    header: "Actions",
    id: "actions",
  }),
];

export function accountStatusLabel(accountStatus: AccountStatus) {
  return accountStatus === "ENABLED" ? "Enabled" : "Disabled";
}

export function UserManagementTable({ users }: { users: Array<UserSummary> }) {
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
