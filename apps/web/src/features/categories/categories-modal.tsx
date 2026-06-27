import { Pill, PillsInput, Text } from "@mantine/core";
import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import {
  createPromptCategoryMutation,
  deletePromptCategoryMutation,
  listPromptCategoriesOptions,
  listPromptCategoriesQueryKey,
  ListPromptCategoriesResponse,
} from "@prompt-vault/api-client";
import { getHotkeyHandler } from "@mantine/hooks";
import { useState } from "react";
import { showNotification } from "@mantine/notifications";

export function CategoriesModal() {
  const client = useQueryClient();
  const categories = useSuspenseQuery(listPromptCategoriesOptions());
  const deleteCategories = useMutation({
    ...deletePromptCategoryMutation(),
    onSuccess: () => {
      categories.refetch();
    },
    onError: (res) => {
      if ("status" in res && res.status === 409) {
        showNotification({
          color: "red",
          message: "This category cannot be deleted because is in use",
        });
      }
    },
  });
  const addCategories = useMutation({
    ...createPromptCategoryMutation(),
    onSuccess: (newCategory) => {
      client.setQueryData<ListPromptCategoriesResponse>(listPromptCategoriesQueryKey(), (data) => [
        ...(data ?? []),
        newCategory,
      ]);
      categories.refetch();
      setNewCategory("");
    },
  });
  const isLoading = [
    categories.isLoading,
    deleteCategories.isPending,
    addCategories.isPending,
  ].some(Boolean);
  const [newCategory, setNewCategory] = useState("");
  return (
    <>
      <Text size={"sm"}></Text>
      <PillsInput label="Enter items" loading={isLoading}>
        <Pill.Group>
          {categories.data.map((c) => (
            <Pill
              withRemoveButton
              onRemove={() =>
                deleteCategories.mutate({
                  path: {
                    categoryId: c.id,
                  },
                })
              }
              key={c.id}
            >
              {c.label}
            </Pill>
          ))}
          <PillsInput.Field
            placeholder="Enter new category"
            value={newCategory}
            onChange={(event) => setNewCategory(event.currentTarget.value)}
            onKeyUp={getHotkeyHandler([
              [
                "Enter",
                () =>
                  addCategories.mutate({
                    body: {
                      label: newCategory,
                    },
                  }),
              ],
            ])}
          />
        </Pill.Group>
      </PillsInput>
    </>
  );
}
