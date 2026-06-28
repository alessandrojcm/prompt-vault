import { createFileRoute } from "@tanstack/react-router";
import { Card, SimpleGrid, Title } from "@mantine/core";
import {
  getAllSubmissionsOptions,
  listPromptCategoriesOptions,
  Prompt,
} from "@prompt-vault/api-client";
import { Car } from "@phosphor-icons/react";

export const Route = createFileRoute("/dashboard/prompts/my-submissions")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(
      getAllSubmissionsOptions({
        path: { userId: context.id },
      }),
    );
  },
});

function SubmissionsCard(prompt: Prompt) {
  return <Card></Card>;
}

function RouteComponent() {
  return (
    <>
      <Title mb={4}>Prompt categories</Title>
      <SimpleGrid cols={3}>
        {categories.data.map((c) => (
          <CategoryCard key={c.id} {...c} />
        ))}
      </SimpleGrid>
    </>
  );
}
