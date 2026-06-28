import { createFileRoute, redirect } from '@tanstack/react-router';

import { requireCurrentUser } from '../features/auth/current-user';

export const Route = createFileRoute("/")({
  beforeLoad: redirectRootToAuthDestination,
  component: RootAuthGate,
});

export async function redirectRootToAuthDestination(
  loaderContext: Parameters<typeof requireCurrentUser>[0],
) {
  await requireCurrentUser(loaderContext);
  throw redirect({ replace: true, to: "/dashboard/prompts" });
}

function RootAuthGate() {
  return null;
}
