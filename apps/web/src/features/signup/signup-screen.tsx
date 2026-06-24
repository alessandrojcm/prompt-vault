import { signupMutation, vSignupRequest } from "@prompt-vault/api-client";
import type { AnyFieldApi } from "@tanstack/react-form";
import { useForm } from "@tanstack/react-form";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";

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
      onChange: vSignupRequest,
      onSubmitAsync: async ({ value, formApi }) => {
        await signup.mutateAsync({ body: value });
        await formApi.reset();
        await navigate({ to: "/signup/success" });
      },
    },
  });

  return (
    <main
      style={{ fontFamily: "sans-serif", margin: "0 auto", maxWidth: 720, padding: "4rem 1.5rem" }}
    >
      <p style={{ letterSpacing: "0.08em", textTransform: "uppercase" }}>Prompt Vault</p>
      <h1>Sign up</h1>
      <p>
        Create your Prompt Vault user. Signup creates an enabled normal user and does not log you
        in.
      </p>
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          event.stopPropagation();
          form.handleSubmit();
        }}
        style={{ display: "grid", gap: "1rem" }}
      >
        <form.Field
          name="username"
          children={(field) => (
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Username</span>
              <input
                name={field.name}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(event) => field.handleChange(event.target.value)}
              />
              <FieldInfo field={field} />
            </label>
          )}
        />

        <form.Field
          name="emailAddress"
          children={(field) => (
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Email Address</span>
              <input
                name={field.name}
                type="email"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(event) => field.handleChange(event.target.value)}
              />
              <FieldInfo field={field} />
            </label>
          )}
        />

        <form.Field
          name="password"
          children={(field) => (
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Password</span>
              <input
                name={field.name}
                type="password"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(event) => field.handleChange(event.target.value)}
              />
              <FieldInfo field={field} />
            </label>
          )}
        />

        <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting]}>
          {([canSubmit, isSubmitting]) => (
            <>
              <button disabled={!canSubmit || isSubmitting || signup.isPending} type="submit">
                {isSubmitting || signup.isPending ? "Creating user..." : "Create account"}
              </button>
            </>
          )}
        </form.Subscribe>
      </form>
    </main>
  );
}

function FieldInfo({ field }: { field: AnyFieldApi }) {
  if (field.state.meta.isTouched && !field.state.meta.isValid) {
    return (
      <ul style={{ color: "crimson", margin: 0, paddingInlineStart: "1.25rem" }}>
        {field.state.meta.errors.map((error) => (
          <li key={String(error)}>{String(error)}</li>
        ))}
      </ul>
    );
  }

  if (field.state.meta.isValidating) {
    return <span>Validating...</span>;
  }

  return null;
}
