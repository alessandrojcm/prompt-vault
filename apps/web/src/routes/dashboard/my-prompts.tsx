import {
  listMyPromptsOptions,
  listPromptCategoriesOptions,
  type ListPromptCategoriesResponse,
} from "@prompt-vault/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { PromptList } from "../../features/prompts/prompt-list";

export const Route = createFileRoute("/dashboard/my-prompts")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listMyPromptsOptions({ path: { userId: context.id } }));
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());

    return {
      id: context.id,
    };
  },
});

function RouteComponent() {
  const { id } = Route.useLoaderData();
  const prompts = useSuspenseQuery(listMyPromptsOptions({ path: { userId: id } }));
  const categories = useSuspenseQuery({
    ...listPromptCategoriesOptions(),
    select: (data) => new Map((data as ListPromptCategoriesResponse).map((c) => [c.id, c.label])),
  });
  return <PromptList title="My Prompts" categoriesMap={categories.data} prompts={prompts.data} />;
}
