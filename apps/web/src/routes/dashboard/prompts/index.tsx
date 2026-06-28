import {
  listMyPromptsOptions,
  listPromptCategoriesOptions,
  type ListPromptCategoriesResponse,
  listPromptsOptions,
} from "@prompt-vault/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { SimpleGrid, Tabs, Title } from "@mantine/core";
import { PromptCard } from "../../../features/prompts/prompt-card";

export const Route = createFileRoute("/dashboard/prompts/")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listPromptsOptions({ query: { visibility: ["PUBLIC"] } }));
    context.queryClient.ensureQueryData(listMyPromptsOptions({ path: { userId: context.id } }));
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());

    return {
      id: context.id,
    };
  },
});

function RouteComponent() {
  const { id } = Route.useLoaderData();
  const publicPrompts = useSuspenseQuery(listPromptsOptions({ query: { visibility: ["PUBLIC"] } }));
  const myPrompts = useSuspenseQuery(listMyPromptsOptions({ path: { userId: id } }));
  const categories = useSuspenseQuery({
    ...listPromptCategoriesOptions(),
    select: (data) => new Map((data as ListPromptCategoriesResponse).map((c) => [c.id, c.label])),
  });

  return (
    <>
      <Title mb={4}>Prompts</Title>
      <Tabs defaultValue="public">
        <Tabs.List>
          <Tabs.Tab value="public">Public</Tabs.Tab>
          <Tabs.Tab value="my-prompts">My private prompts</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="public">
          <SimpleGrid mt={3} cols={2}>
            {publicPrompts.data.map((p) => (
              <PromptCard categoryLabel={categories.data.get(p.categoryId)!} key={p.id} {...p} />
            ))}
          </SimpleGrid>
        </Tabs.Panel>

        <Tabs.Panel value="my-prompts">
          <SimpleGrid mt={3} cols={3}>
            {myPrompts.data.map((p) => (
              <PromptCard categoryLabel={categories.data.get(p.categoryId)!} key={p.id} {...p} />
            ))}
          </SimpleGrid>
        </Tabs.Panel>
      </Tabs>
    </>
  );
}
