import { useMemo } from 'react';

import { AdminFlaggedPrompt, ListAdminFlaggedPromptsResponse } from '@prompt-vault/api-client';
import { Badge, Card, Pill, Stack, Table, Text, Title } from '@mantine/core';
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  useReactTable,
} from '@tanstack/react-table';

const columnHelper = createColumnHelper<AdminFlaggedPrompt>();

export function FlaggedPromptsTable({
  flaggedPrompts,
}: {
  flaggedPrompts: ListAdminFlaggedPromptsResponse;
}) {
  const flaggedPromptsColumn = useMemo(
    () => [
      columnHelper.accessor("title", {
        cell: (info) => info.getValue(),
        header: "Title",
      }),
      columnHelper.accessor("ownerUsername", {
        cell: (info) => info.getValue(),
        header: "Prompt Owner",
      }),
      columnHelper.accessor("categoryLabel", {
        cell: (info) => <Badge variant="light">{info.getValue()}</Badge>,
        header: "Category",
      }),
      columnHelper.accessor("matchedKeywordSnapshots", {
        cell: (info) =>
          info.getValue().map((k) => (
            <Pill style={{ textTransform: "capitalize" }} key={k}>
              {k}
            </Pill>
          )),
        header: "Flagged keywords",
      }),
      columnHelper.accessor("submittedAt", {
        cell: (info) => new Date(info.getValue()).toLocaleDateString(),
        header: "Date of submitssion",
      }),
    ],
    [],
  );

  const table = useReactTable({
    columns: flaggedPromptsColumn,
    data: flaggedPrompts,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
  });

  return (
    <Card aria-label="Flagged Prompts" padding="xl" radius="md" shadow="sm" withBorder>
      <Stack gap="lg">
        <div>
          <Text c="dimmed" fw={700} size="xs" tt="uppercase">
            Admin
          </Text>
          <Title order={2}>Flagged Prompts</Title>
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
