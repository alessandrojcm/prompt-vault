import { createFileRoute } from "@tanstack/react-router";
import { Drawer, SimpleGrid, Text, Timeline, Title } from "@mantine/core";
import {
  getAllSubmissionsOptions,
  listPromptCategoriesOptions,
  type ListPromptCategoriesResponse,
  listPromptsSubmissionOptions,
  Prompt,
} from "@prompt-vault/api-client";
import { PaperPlaneIcon } from "@phosphor-icons/react";
import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { PromptCard } from "../../../features/prompts/prompt-card";
import { useDisclosure, UseDisclosureReturnValue } from "@mantine/hooks";
import { useState } from "react";

export const Route = createFileRoute("/dashboard/prompts/my-submissions")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(
      getAllSubmissionsOptions({
        path: { userId: context.id },
      }),
    );
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());
    return {
      userId: context.id,
    };
  },
});

function SubmissionDrawer({
  id,
  userId,
  disclosure,
}: {
  id: number | undefined;
  userId: number;
  disclosure: UseDisclosureReturnValue;
}) {
  const submissions = useQuery({
    ...listPromptsSubmissionOptions({
      path: {
        promptId: id!,
        userId: userId,
      },
    }),
    enabled: !!id,
  });
  return (
    <Drawer
      title={"Submission History"}
      opened={disclosure[0] && submissions.isSuccess}
      onClose={disclosure[1].close}
    >
      <Timeline active={1} bulletSize={24} lineWidth={2}>
        {submissions.data?.map((s) => (
          <Timeline.Item
            key={s.submittedAt}
            bullet={<PaperPlaneIcon size={12} />}
            title={`Submission response`}
          >
            <Text c="dimmed" size="sm">
              {s.response}
            </Text>
            <Text size="xs" mt={4}>
              {new Date(s.submittedAt).toLocaleDateString()}
            </Text>
          </Timeline.Item>
        ))}
      </Timeline>
    </Drawer>
  );
}

function RouteComponent() {
  const { userId } = Route.useLoaderData();
  const submissions = useSuspenseQuery(
    getAllSubmissionsOptions({
      path: { userId: userId },
    }),
  );
  const categories = useSuspenseQuery({
    ...listPromptCategoriesOptions(),
    select: (data) => new Map((data as ListPromptCategoriesResponse).map((c) => [c.id, c.label])),
  });
  const [selectPrompt, setSelectedPrompt] = useState<Prompt | null>(null);
  const disclosure = useDisclosure(false, {
    onClose: () => setSelectedPrompt(null),
  });
  return (
    <>
      <Title mb={4}>Prompt categories</Title>
      <SimpleGrid cols={3}>
        {submissions.data.map((c) => (
          <PromptCard
            key={c.id}
            {...c}
            categoryLabel={categories.data.get(c.categoryId)!}
            enableEditing={false}
            enableSubmission={false}
            onClick={() => {
              setSelectedPrompt(c);
              disclosure[1].open();
            }}
          />
        ))}
      </SimpleGrid>
      <SubmissionDrawer id={selectPrompt?.id} userId={userId} disclosure={disclosure} />
    </>
  );
}
