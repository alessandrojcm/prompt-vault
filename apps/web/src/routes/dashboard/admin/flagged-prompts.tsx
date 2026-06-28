import { Stack } from "@mantine/core";
import { listAdminFlaggedPromptsOptions } from "@prompt-vault/api-client";
import { createFileRoute } from "@tanstack/react-router";
import { FlaggedPromptsTable } from "../../../features/admin/flagged-prompts";
import { useSuspenseQuery } from "@tanstack/react-query";

export const Route = createFileRoute("/dashboard/admin/flagged-prompts")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listAdminFlaggedPromptsOptions());
  },
});

function RouteComponent() {
  const flaggedPrompts = useSuspenseQuery(listAdminFlaggedPromptsOptions());
  return (
    <Stack gap="md">
      <FlaggedPromptsTable flaggedPrompts={flaggedPrompts.data ?? []} />
    </Stack>
  );
}
