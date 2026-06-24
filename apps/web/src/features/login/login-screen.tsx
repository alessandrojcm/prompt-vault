import {
  getCurrentUserQueryKey,
  LoginError,
  loginMutation,
  vLoginRequest,
} from "@prompt-vault/api-client";
import {
  Alert,
  Anchor,
  Button,
  Card,
  Fieldset,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import type { AnyFieldApi } from "@tanstack/react-form";
import { useForm } from "@tanstack/react-form";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useRouter } from "@tanstack/react-router";

type LoginFormState = {
  username: string;
  password: string;
};

const initialFormState: LoginFormState = {
  username: "",
  password: "",
};

export function LoginScreen() {
  const navigate = useNavigate();
  const router = useRouter();
  const queryClient = useQueryClient();
  const login = useMutation(loginMutation());

  const form = useForm({
    defaultValues: initialFormState,
    validators: {
      onSubmit: vLoginRequest,
      onSubmitAsync: async ({ value }) => {
        const currentUser = await login.mutateAsync({ body: value });
        queryClient.setQueryData(getCurrentUserQueryKey(), currentUser);
        await router.invalidate();
        await navigate({ to: "/dashboard" });
      },
    },
  });

  return (
    <Card maw={520} mx="auto" padding="xl" radius="md" shadow="sm" withBorder>
      <Stack gap="lg">
        <div>
          <Text c="dimmed" fw={700} size="xs" tt="uppercase">
            Prompt Vault
          </Text>
          <Title order={2}>Log in</Title>
        </div>
        {login.isError ? (
          <Alert color="red" title="Could not log in" variant="light">
            {loginErrorMessage(login.error)}
          </Alert>
        ) : null}
        <form
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            event.stopPropagation();
            form.handleSubmit();
          }}
        >
          <Fieldset>
            <Stack gap="md">
              <form.Field
                name="username"
                children={(field) => (
                  <TextInput
                    error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                    label="Username"
                    name={field.name}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                )}
              />

              <form.Field
                name="password"
                children={(field) => (
                  <PasswordInput
                    error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                    label="Password"
                    name={field.name}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                )}
              />

              <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]}>
                {([canSubmit, isSubmitting]) => (
                  <Button disabled={!canSubmit || isSubmitting || login.isPending} type="submit">
                    {isSubmitting || login.isPending ? "Logging in..." : "Log in"}
                  </Button>
                )}
              </form.Subscribe>
            </Stack>
          </Fieldset>
        </form>
        <Text c="dimmed" size="sm">
          Don't have an account?&nbsp;
          <Anchor component={Link} to="/signup">
            Sign up
          </Anchor>
        </Text>
      </Stack>
    </Card>
  );
}

export function loginErrorMessage(error: unknown) {
  return (error as LoginError)?.message ?? "Invalid username or password.";
}

function FieldInfo({ field }: { field: AnyFieldApi }) {
  if (field.state.meta.isTouched && !field.state.meta.isValid) {
    return field.state.meta.errors.map((error) => String(error.message)).join(", ");
  }

  if (field.state.meta.isValidating) {
    return "Validating...";
  }

  return null;
}
