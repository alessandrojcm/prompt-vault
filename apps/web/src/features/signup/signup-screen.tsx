import { SignupError, signupMutation, vSignupRequest } from "@prompt-vault/api-client";
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
import { useMutation } from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";

type SignupFormState = {
  username: string;
  emailAddress: string;
  password: string;
};

const initialFormState: SignupFormState = {
  username: "",
  emailAddress: "",
  password: "",
};

export function SignupScreen() {
  const navigate = useNavigate();
  const signup = useMutation(signupMutation());

  const form = useForm({
    defaultValues: initialFormState,
    validators: {
      onSubmit: vSignupRequest,
      onSubmitAsync: async ({ value, formApi }) => {
        try {
          await signup.mutateAsync({ body: value });
          await formApi.reset();
          await navigate({ to: "/signup/success" });
        } catch (error) {
          return {
            form: (error as SignupError)?.message,
            fields: (error as SignupError).fieldErrors.reduce(
              (curr, prv) => ({ ...curr, [prv.field]: prv.message }),
              {},
            ),
          };
        }
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
          <Title order={2}>Sign up</Title>
        </div>
        <Text c="dimmed">
          Create your Prompt Vault user. Signup creates an enabled normal user.
        </Text>
        {signup.isError ? (
          <Alert color="red" title="Could not create account" variant="light">
            {signup.error instanceof Error ? signup.error.message : "Please check your details."}
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
                name="emailAddress"
                children={(field) => (
                  <TextInput
                    error={!field.state.meta.isValid ? <FieldInfo field={field} /> : null}
                    label="Email address"
                    name={field.name}
                    type="email"
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
                  <Button disabled={!canSubmit || isSubmitting || signup.isPending} type="submit">
                    {isSubmitting || signup.isPending ? "Creating user..." : "Create account"}
                  </Button>
                )}
              </form.Subscribe>
            </Stack>
          </Fieldset>
        </form>
        <Text c="dimmed" size="sm">
          Already have an account?&nbsp;
          <Anchor component={Link} to="/login">
            Log in
          </Anchor>
        </Text>
      </Stack>
    </Card>
  );
}

function FieldInfo({ field }: { field: AnyFieldApi }) {
  if (field.state.meta.isTouched && !field.state.meta.isValid) {
    return field.state.meta.errors.map((error) => error?.message ?? error).join(", ");
  }

  if (field.state.meta.isValidating) {
    return "Validating...";
  }

  return null;
}
