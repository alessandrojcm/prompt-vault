import { getCurrentUser, type CurrentUserState } from "@prompt-vault/api-client";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "./query-keys";

export type UserSessionViewState = "signed-in" | "signed-out";

export function toUserSessionViewState(currentUser: CurrentUserState): UserSessionViewState {
  return currentUser.authenticated ? "signed-in" : "signed-out";
}

export const currentUserQueryOptions = () =>
  queryOptions({
    queryKey: QueryKeys.currentUser(),
    queryFn: async (): Promise<UserSessionViewState> =>
      toUserSessionViewState(await getCurrentUser()),
  });
