import { Link, createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/signup/success")({
  component: SignupSuccessPage,
});

function SignupSuccessPage() {
  return (
    <main
      style={{ fontFamily: "sans-serif", margin: "0 auto", maxWidth: 720, padding: "4rem 1.5rem" }}
    >
      <p style={{ letterSpacing: "0.08em", textTransform: "uppercase" }}>Prompt Vault</p>
      <h1>Signup complete</h1>
      <p>Your user has been created. You can log in once the login flow is available.</p>
      <Link to="/signup">Back to signup</Link>
    </main>
  );
}
