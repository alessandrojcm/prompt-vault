import { UseDisclosureReturnValue } from "@mantine/hooks";
import {
  Modal,
  TextInput,
  Textarea,
  Fieldset,
  Stack,
  Autocomplete,
  Switch,
  Button,
} from "@mantine/core";
import {
  createPromptMutation,
  CreatePromptRequest,
  GetCurrentUserResponse,
  listMyPromptsOptions,
  ListPromptCategoriesResponse,
  listPublicPromptsOptions,
  ValidationErrorResponse,
  vCreatePromptRequest,
} from "@prompt-vault/api-client";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { showNotification } from "@mantine/notifications";
import { useForm } from "@tanstack/react-form";
import { FieldInfo } from "../../components/field-info";
import { useMemo } from "react";

type Props = {
  disclosure: UseDisclosureReturnValue;
  categories: ListPromptCategoriesResponse;
  currentUser: GetCurrentUserResponse;
};

const defaultValues = {
  title: "",
  text: "",
  categoryId: 0,
  visibility: "PRIVATE",
} as CreatePromptRequest;

export function CreatePrompt(props: Props) {
  const {
    disclosure,
    categories,
    currentUser: { id },
  } = props;
  const [opened, { close }] = disclosure;
  const client = useQueryClient();
  const createPrompt = useMutation({
    ...createPromptMutation(),
    onSuccess: async (data) => {
      showNotification({
        message: "Prompt created successfully",
        position: "top-right",
      });
      await client.invalidateQueries(listMyPromptsOptions({ path: { userId: id } }));
      if (data.visibility === "PRIVATE") {
        await client.invalidateQueries(listPublicPromptsOptions());
      }
      close();
    },
    onError: (error) => {
      console.error(error);
      showNotification({
        message: "Failed to create prompt",
        color: "red",
        position: "top-right",
      });
    },
  });

  const categoriesMap = useMemo(
    () => new Map(categories?.map((c) => [c.id, c.label]) ?? []),
    [categories],
  );

  const form = useForm({
    defaultValues,
    validators: {
      onSubmit: vCreatePromptRequest,
      onSubmitAsync: async ({ value, formApi }) => {
        try {
          await createPrompt.mutateAsync({ body: value });
          formApi.reset();
        } catch (error) {
          if (!(error instanceof Object && "fieldErrors" in error)) {
            showNotification({
              message: "Failed to create prompt",
              color: "red",
            });
            return;
          }
          return {
            form: (error as ValidationErrorResponse)?.message,
            fields: (error as ValidationErrorResponse).fieldErrors.reduce(
              (curr, prv) => ({
                ...curr,
                [prv.field]: prv.message,
              }),
              {},
            ),
          };
        }
      },
    },
  });

  return (
    <Modal opened={opened} onClose={close} title="Create new prompt" centered>
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          event.stopPropagation();
          form.handleSubmit();
        }}
      >
        <Fieldset disabled={form.state.isSubmitting}>
          <Stack gap="lg">
            <form.Field
              name="title"
              children={(field) => (
                <TextInput
                  error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                  label="Prompt title"
                  name={field.name}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                />
              )}
            />
            <form.Field
              name="categoryId"
              children={(field) => (
                <Autocomplete
                  error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                  label="Prompt category"
                  placeholder="Pick a category"
                  data={Array.from(categoriesMap.keys()).map(String)}
                  name={field.name}
                  renderOption={({ option }) => (
                    <div key={option.value}>{categoriesMap.get(Number(option.value))}</div>
                  )}
                  value={categoriesMap.get(Number(field.state.value))}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(Number(event))}
                />
              )}
            />
            <form.Field
              name="text"
              children={(field) => (
                <Textarea
                  autosize
                  resize="vertical"
                  minRows={8}
                  error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                  label="Prompt text"
                  name={field.name}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(event) => field.handleChange(event.target.value)}
                />
              )}
            />
            <form.Field
              name="visibility"
              children={(field) => (
                <Switch
                  size="xl"
                  label="Visibility"
                  name={field.name}
                  value={field.state.value}
                  onLabel={"Public"}
                  defaultChecked={false}
                  offLabel={"Private"}
                  onChange={(event) => {
                    field.handleChange(event.currentTarget.checked ? "PUBLIC" : "PRIVATE");
                  }}
                />
              )}
            />
          </Stack>

          <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]}>
            {([canSubmit, isSubmitting]) => (
              <Button
                mt={4}
                disabled={!canSubmit || isSubmitting || createPrompt.isPending}
                type="submit"
              >
                {isSubmitting || createPrompt.isPending ? "Creating prompt..." : "Create prompt"}
              </Button>
            )}
          </form.Subscribe>
        </Fieldset>
      </form>
    </Modal>
  );
}
