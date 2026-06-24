import type { QueryFunctionContext } from "@tanstack/react-query";
import { QueryClient } from "@tanstack/react-query";
import { isRedirect } from "@tanstack/react-router";
import { afterEach, describe, expect, it, vi } from "vitest";

let currentUser: unknown = { username: "alex" };
let currentUserError: unknown;

vi.mock("@prompt-vault/api-client", () => ({
  getCurrentUserOptions: ({ signal }: { signal: AbortSignal }) => ({
    queryKey: ["currentUser"],
    queryFn: async (_context: QueryFunctionContext) => {
      if (signal.aborted) {
        throw new Error("aborted");
      }

      if (currentUserError) {
        throw currentUserError;
      }

      return currentUser;
    },
  }),
}));

import { requireCurrentUser } from "./current-user";
import { redirectRootToAuthDestination } from "../../routes/index";

describe("requireCurrentUser", () => {
  afterEach(() => {
    currentUser = { username: "alex" };
    currentUserError = undefined;
  });

  it("returns the current user when the session is authenticated", async () => {
    const queryClient = new QueryClient();

    await expect(
      requireCurrentUser({
        abortController: new AbortController(),
        context: { queryClient },
      }),
    ).resolves.toEqual({ username: "alex" });
  });

  it("redirects unauthenticated sessions to login", async () => {
    const queryClient = new QueryClient();
    currentUserError = new Error("unauthenticated");

    try {
      await requireCurrentUser({
        abortController: new AbortController(),
        context: { queryClient },
      });
      throw new Error("Expected requireCurrentUser to redirect");
    } catch (error) {
      expect(isRedirect(error)).toBe(true);
      expect(error).toMatchObject({ options: { replace: true, to: "/login" } });
    }
  });
});

describe("redirectRootToAuthDestination", () => {
  afterEach(() => {
    currentUser = { username: "alex" };
    currentUserError = undefined;
  });

  it("redirects authenticated sessions from root to dashboard", async () => {
    const queryClient = new QueryClient();

    try {
      await redirectRootToAuthDestination({
        abortController: new AbortController(),
        context: { queryClient },
      });
      throw new Error("Expected redirectRootToAuthDestination to redirect");
    } catch (error) {
      expect(isRedirect(error)).toBe(true);
      expect(error).toMatchObject({ options: { replace: true, to: "/dashboard" } });
    }
  });

  it("redirects unauthenticated sessions from root to login", async () => {
    const queryClient = new QueryClient();
    currentUserError = new Error("unauthenticated");

    try {
      await redirectRootToAuthDestination({
        abortController: new AbortController(),
        context: { queryClient },
      });
      throw new Error("Expected redirectRootToAuthDestination to redirect");
    } catch (error) {
      expect(isRedirect(error)).toBe(true);
      expect(error).toMatchObject({ options: { replace: true, to: "/login" } });
    }
  });
});
