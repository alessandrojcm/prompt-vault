import type { UserSessionViewState } from '../../lib/current-user';

type UserSessionShellProps = {
  state: UserSessionViewState;
};

export function UserSessionShell({ state }: UserSessionShellProps) {
  return (
    <main
      style={{ fontFamily: "sans-serif", margin: "0 auto", maxWidth: 720, padding: "4rem 1.5rem" }}
    >
      <p style={{ letterSpacing: "0.08em", textTransform: "uppercase" }}>Prompt Vault</p>
      <h1>Auth slice tracer</h1>
      <p>
        This shell exercises the current-user contract from the TanStack Start app through the
        Spring Boot API.
      </p>
      <section>
        <strong>User status:</strong>{" "}
        {state === "signed-in" ? "Authenticated session detected" : "No authenticated session"}
      </section>
    </main>
  );
}
