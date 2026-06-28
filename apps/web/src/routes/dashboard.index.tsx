import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/dashboard/")({
  component: DashboardPage,
  beforeLoad: () => {
    throw redirect({
      to: "/dashboard/prompts",
    });
  },
});

function DashboardPage() {
  return null;
}
