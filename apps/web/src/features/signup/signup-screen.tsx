import { signupMutation, type SignupError } from "@prompt-vault/api-client";
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
      onSubmitAsync: async ({ value }) => {
        try {
          await signup.mutateAsync({ body: value });
          await navigate({ to: "/signup/success" });

          return undefined;
        } catch (error) {
          const signupError = error as SignupError;
// TODO: prettys ure we can avoid this
          if (isSignupValidationError(signupError)) {
            return {
              form: signupError.message,
              fields: Object.fromEntries(
                signupError.fieldErrors.map((fieldError) => [fieldError.field, fieldError.message]),
              ),
            };
          }

          return {
            form: "Signup failed unexpectedly.",
          };
        }
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
          validators={{
            onChange: ({ value }) => {
              const username = value.trim();

              if (!username) {
                return "Username is required.";
              }

              if (username.length < 3 || username.length > 30) {
                return "Username must be 3 to 30 characters long.";
              }

              if (!/^[A-Za-z0-9._-]+$/.test(username)) {
                return "Username may only contain letters, numbers, dots, hyphens, and underscores.";
              }

              return undefined;
            },
          }}
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
          validators={{
            onChange: ({ value }) => {
              const emailAddress = value.trim();

              if (!emailAddress) {
                return "Email Address is required.";
              }

              if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailAddress)) {
                return "Email Address must be valid.";
              }

              return undefined;
            },
          }}
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
          validators={{
            onChange: ({ value }) => {
              if (!value || !value.trim()) {
                return "Password is required.";
              }

              if (value.length < 8) {
                return "Password must be at least 8 characters long.";
              }

              return undefined;
            },
          }}
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

function isSignupValidationError(error: SignupError | unknown): error is SignupError {
  return (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    "fieldErrors" in error &&
    Array.isArray((error as SignupError).fieldErrors)
  );
}
