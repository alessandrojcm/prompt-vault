import { getCurrentUserOptions } from "@prompt-vault/api-client";
import type { QueryClient } from "@tanstack/react-query";
import { redirect } from "@tanstack/react-router";

type CurrentUserRouteContext = {
  context: {
    queryClient: QueryClient;
  };
  abortController: AbortController;
};

export async function requireCurrentUser({ context, abortController }: CurrentUserRouteContext) {
  try {
    return await context.queryClient.ensureQueryData(
      getCurrentUserOptions({
        signal: abortController.signal,
      }),
    );
  } catch {
    throw redirect({ replace: true, to: "/login" });
  }
}
