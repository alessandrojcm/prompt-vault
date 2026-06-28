import { QueryClient } from '@tanstack/react-query';
import { isRedirect } from '@tanstack/react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { requireAdminUser, requireCurrentUser } from './current-user';
import { redirectRootToAuthDestination } from '../../routes/index';

let currentUser: unknown = { role: "USER", username: "alex" };
let currentUserError: unknown;

vi.mock("@prompt-vault/api-client", () => ({
  getCurrentUserOptions: ({ signal }: { signal: AbortSignal }) => ({
    queryKey: ["currentUser"],
    queryFn: async () => {
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

describe("requireCurrentUser", () => {
  afterEach(() => {
    currentUser = { role: "USER", username: "alex" };
    currentUserError = undefined;
  });

  it("returns the current user when the session is authenticated", async () => {
    const queryClient = new QueryClient();

    await expect(
      requireCurrentUser({
        abortController: new AbortController(),
        context: { queryClient },
      }),
    ).resolves.toEqual({ role: "USER", username: "alex" });
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

describe("requireAdminUser", () => {
  afterEach(() => {
    currentUser = { role: "USER", username: "alex" };
    currentUserError = undefined;
  });

  it("returns the current user when they are an admin", async () => {
    const queryClient = new QueryClient();
    currentUser = { role: "ADMIN", username: "admin" };

    await expect(
      requireAdminUser({
        abortController: new AbortController(),
        context: { queryClient },
      }),
    ).resolves.toEqual({ role: "ADMIN", username: "admin" });
  });

  it("redirects authenticated normal users to the dashboard", async () => {
    const queryClient = new QueryClient();

    try {
      await requireAdminUser({
        abortController: new AbortController(),
        context: { queryClient },
      });
      throw new Error("Expected requireAdminUser to redirect");
    } catch (error) {
      expect(isRedirect(error)).toBe(true);
      expect(error).toMatchObject({
        options: { replace: true, to: "/dashboard" },
      });
    }
  });
});

describe("redirectRootToAuthDestination", () => {
  afterEach(() => {
    currentUser = { role: "USER", username: "alex" };
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
      expect(error).toMatchObject({
        options: { replace: true, to: "/dashboard/prompts" },
      });
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
