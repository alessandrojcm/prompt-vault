import { createFileRoute } from "@tanstack/react-router";

import { LoginScreen } from "../features/login/login-screen";

export const Route = createFileRoute("/login/")({
  component: LoginPage,
});

function LoginPage() {
  return <LoginScreen />;
}
