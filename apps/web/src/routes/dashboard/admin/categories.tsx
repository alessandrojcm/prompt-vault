import { createFileRoute } from "@tanstack/react-router";
import {
  ActionIcon,
  Button,
  ButtonGroup,
  Card,
  DataList,
  Group,
  SimpleGrid,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from "@mantine/core";
import {
  deletePromptCategoryMutation,
  listPromptCategoriesOptions,
  listPromptCategoriesQueryKey,
  ListPromptCategoriesResponse,
  PromptCategory,
  updatePromptCategoryMutation,
  vUpdatePromptCategoryBody,
} from "@prompt-vault/api-client";
import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { PencilIcon } from "@phosphor-icons/react/dist/ssr";
import { useState } from "react";
import { useForm } from "@tanstack/react-form";
import { showNotification } from "@mantine/notifications";
import { FieldInfo } from "../../../components/field-info";
import { getHotkeyHandler, useClickOutside, useFocusTrap, useMergedRef } from "@mantine/hooks";
import { TrashIcon } from "@phosphor-icons/react";

export const Route = createFileRoute("/dashboard/admin/categories")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());
  },
});

function CategoryCard(props: PromptCategory) {
  const { label, description, id } = props;
  const [edit, setEdit] = useState({
    label: false,
    description: false,
  });
  const client = useQueryClient();
  const updateCategory = useMutation({
    ...updatePromptCategoryMutation(),
    onMutate: (data) => {
      const previousData = client.getQueryData<ListPromptCategoriesResponse>(
        listPromptCategoriesQueryKey(),
      );
      client.setQueryData<ListPromptCategoriesResponse>(listPromptCategoriesQueryKey(), (d) => {
        return (d ?? []).map((c) => (c.id === id ? { ...c, ...data } : c));
      });
      return previousData;
    },
    onError: (_, __, oldData) => {
      client.setQueryData<ListPromptCategoriesResponse>(listPromptCategoriesQueryKey(), oldData);
      showNotification({
        color: "red",
        position: "top-right",
        message: "There was an error updating the category",
      });
    },
    onSuccess: () => {
      client.invalidateQueries(listPromptCategoriesOptions());
    },
  });

  const deleteCategory = useMutation({
    ...deletePromptCategoryMutation(),
    onSuccess: () => {
      client.invalidateQueries(listPromptCategoriesOptions());
    },
    onError: (error) => {
      if ("status" in error && error.status === 409) {
        showNotification({
          color: "red",
          position: "top-right",
          message: "This category cannot be deleted because is in use",
        });
        return;
      }
      showNotification({
        color: "red",
        position: "top-right",
        message: "There was an error deleting this category",
      });
    },
  });

  const form = useForm({
    defaultValues: {
      label,
      description,
    },
    validators: {
      onSubmit: vUpdatePromptCategoryBody,
      onSubmitAsync: async ({ value }) => {
        await updateCategory.mutateAsync({
          path: {
            categoryId: id,
          },
          body: {
            ...value,
          },
        });
        setEdit({
          label: false,
          description: false,
        });
      },
    },
  });
  const focusTrapRef = useFocusTrap();
  const clickOutside = useClickOutside(() =>
    setEdit((cur) => ({
      label: cur.label ? false : cur.label,
      description: cur.description ? false : cur.description,
    })),
  );
  const mergedRefs = useMergedRef(focusTrapRef, clickOutside);
  const submission = getHotkeyHandler([["Enter", () => form.handleSubmit()]]);

  return (
    <Card shadow="md" withBorder h={"auto"}>
      <form
        style={{ width: "100%" }}
        noValidate
        onSubmit={(event) => {
          if (!edit) {
            return;
          }
          event.preventDefault();
          event.stopPropagation();
        }}
      >
        <DataList orientation={"vertical"}>
          <DataList.Item>
            <DataList.ItemLabel>
              <Group>
                Name
                <ActionIcon variant={"subtle"} aria-label={"edit category name"}>
                  <PencilIcon
                    size={16}
                    onClick={() => {
                      setEdit((cur) => ({
                        ...cur,
                        label: true,
                      }));
                    }}
                  />
                </ActionIcon>
                <ActionIcon
                  aria-label={"delete category"}
                  ml={"auto"}
                  color={"red"}
                  variant={"subtle"}
                  onClick={() => deleteCategory.mutate({ path: { categoryId: id } })}
                >
                  <TrashIcon size={16} />
                </ActionIcon>
              </Group>
            </DataList.ItemLabel>
            <DataList.ItemValue>
              {edit.label ? (
                <form.Field
                  name="label"
                  children={(field) => (
                    <TextInput
                      onKeyDown={submission}
                      ref={mergedRefs}
                      disabled={form.state.isSubmitting}
                      error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                    />
                  )}
                />
              ) : (
                <Text size={"sm"}>{form.getFieldValue("label")}</Text>
              )}
            </DataList.ItemValue>
          </DataList.Item>
          <DataList.Item>
            <DataList.ItemLabel>
              Description
              <ActionIcon variant={"subtle"} aria-label={"edit category description"}>
                <PencilIcon
                  size={16}
                  onClick={() => {
                    setEdit((cur) => ({
                      ...cur,
                      description: true,
                    }));
                  }}
                />
              </ActionIcon>
            </DataList.ItemLabel>
            <DataList.ItemValue>
              {edit.description ? (
                <form.Field
                  name="description"
                  children={(field) => (
                    <Textarea
                      onKeyDown={submission}
                      ref={mergedRefs}
                      disabled={form.state.isSubmitting}
                      resize="vertical"
                      rows={3}
                      error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                      name={field.name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) => field.handleChange(event.target.value)}
                    />
                  )}
                />
              ) : (
                <Text>{form.getFieldValue("description")}</Text>
              )}
            </DataList.ItemValue>
          </DataList.Item>
        </DataList>
      </form>
    </Card>
  );
}

function RouteComponent() {
  const categories = useSuspenseQuery(listPromptCategoriesOptions());
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
