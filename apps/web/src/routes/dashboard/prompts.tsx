import {
  listPromptCategoriesOptions,
  type ListPromptCategoriesResponse,
  listPublicPromptsOptions,
} from "@prompt-vault/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { PromptList } from "../../features/prompts/prompt-list";

export const Route = createFileRoute("/dashboard/prompts")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listPublicPromptsOptions());
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());

    return {
      id: context.id,
    };
  },
});

function RouteComponent() {
  const prompts = useSuspenseQuery(listPublicPromptsOptions());
  const categories = useSuspenseQuery({
    ...listPromptCategoriesOptions(),
    select: (data) => new Map((data as ListPromptCategoriesResponse).map((c) => [c.id, c.label])),
  });
  return <PromptList title="Prompts" categoriesMap={categories.data} prompts={prompts.data} />;
}
