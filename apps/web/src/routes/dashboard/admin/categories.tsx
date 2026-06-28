import { createFileRoute } from '@tanstack/react-router';
import { ActionIcon, Button, Card, Group, SimpleGrid, Stack, Text, Textarea, TextInput, Title, } from '@mantine/core';
import {
  listPromptCategoriesOptions,
  listPromptCategoriesQueryKey,
  ListPromptCategoriesResponse,
  PromptCategory,
  updatePromptCategoryMutation,
  vUpdatePromptCategoryBody,
} from '@prompt-vault/api-client';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { PencilIcon } from '@phosphor-icons/react/dist/ssr';
import { useState } from 'react';
import { useForm } from '@tanstack/react-form';
import { showNotification } from '@mantine/notifications';
import { FieldInfo } from '../../../components/field-info';

export const Route = createFileRoute("/dashboard/admin/categories")({
  component: RouteComponent,
  loader: ({ context }) => {
    context.queryClient.ensureQueryData(listPromptCategoriesOptions());
  },
});

function CategoryCard(props: PromptCategory) {
  const { label, description, id } = props;
  const [edit, setEdit] = useState(false);
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
        setEdit(false);
      },
    },
  });

  return (
    <Card shadow="md" padding="xl">
      {edit ? (
        <form
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            event.stopPropagation();
            form.handleSubmit();
          }}
        >
          <form.Field
            name="label"
            children={(field) => (
              <TextInput
                disabled={form.state.isSubmitting}
                error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                label="Category name"
                name={field.name}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(event) => field.handleChange(event.target.value)}
              />
            )}
          />
          <form.Field
            name="description"
            children={(field) => (
              <Textarea
                disabled={form.state.isSubmitting}
                autosize
                resize="vertical"
                minRows={3}
                error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                label="Category description"
                name={field.name}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(event) => field.handleChange(event.target.value)}
              />
            )}
          />

          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]}>
            {([canSubmit, isSubmitting]) => (
              <Group justify={"space-between"}>
                <Button mt={4} onClick={() => setEdit(false)} variant="outline" color="red">
                  Cancel
                </Button>
                <Button mt={4} disabled={!canSubmit || isSubmitting} type="submit">
                  {isSubmitting ? "Updating category..." : "Update category"}
                </Button>
              </Group>
            )}
          </form.Subscribe>
        </form>
      ) : (
        <Stack justify={"space-between"} h={"100%"}>
          <Group>
            {" "}
            <Title order={2}>{label}</Title>
            <ActionIcon
              aria-label={"Edit category"}
              variant={"subtle"}
              onClick={() => setEdit(true)}
            >
              <PencilIcon />
            </ActionIcon>
          </Group>
          <Text>{description}</Text>
        </Stack>
      )}
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
