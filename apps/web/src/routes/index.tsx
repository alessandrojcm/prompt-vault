import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";

import { UserSessionShell } from "../features/user-session/user-session-shell";
import { currentUserQueryOptions } from "../lib/current-user";

export const Route = createFileRoute("/")({
  loader: ({ context }) => context.queryClient.ensureQueryData(currentUserQueryOptions()),
  component: HomePage,
});

function HomePage() {
  const { data: userSession } = useSuspenseQuery(currentUserQueryOptions());

  return <UserSessionShell state={userSession} />;
}
