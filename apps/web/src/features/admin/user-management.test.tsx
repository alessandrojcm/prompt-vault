import { listAdminUsersOptions, type UserSummary } from "@prompt-vault/api-client";
import { MantineProvider } from "@mantine/core";
import { Notifications, notifications } from "@mantine/notifications";
import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { render } from "vitest-browser-react";

import { UserManagementTable, accountStatusLabel } from "./user-management";

describe("UserManagementTable", () => {
  it("shows normal-user management columns and hides internal ids and roles", async () => {
    const screen = await render(
      <UserManagementTableTestApp
        users={[
          {
            accountStatus: "ENABLED",
            emailAddress: "alice@example.com",
            id: 42,
            role: "USER",
            username: "alice",
          },
        ]}
      />,
    );

    await expect.element(screen.getByText("Username")).toBeVisible();
    await expect.element(screen.getByText("Email address")).toBeVisible();
    await expect.element(screen.getByText("Account status")).toBeVisible();
    await expect.element(screen.getByText("Actions")).toBeVisible();
    await expect.element(screen.getByText("Role")).not.toBeInTheDocument();
    await expect.element(screen.getByText("ID")).not.toBeInTheDocument();

    await expect.element(screen.getByRole("cell", { exact: true, name: "alice" })).toBeVisible();
    await expect.element(screen.getByText("alice@example.com")).toBeVisible();
    await expect.element(screen.getByText("Enabled")).toBeVisible();
    await expect.element(screen.getByRole("button", { name: "Disable" })).toBeVisible();
    await expect.element(screen.getByText("42")).not.toBeInTheDocument();
    await expect.element(screen.getByText("USER", { exact: true })).not.toBeInTheDocument();
  });

  it("enables disabled users immediately and updates the row in place", async () => {
    const showNotification = vi.spyOn(notifications, "show");
    const updateUserStatus = vi.fn().mockResolvedValue({
      accountStatus: "ENABLED",
      emailAddress: "alice@example.com",
      id: 42,
      role: "USER",
      username: "alice",
    });
    const screen = await render(
      <UserManagementTableTestApp
        updateUserStatus={updateUserStatus}
        users={[
          {
            accountStatus: "DISABLED",
            emailAddress: "alice@example.com",
            id: 42,
            role: "USER",
            username: "alice",
          },
        ]}
      />,
    );

    await screen.getByRole("button", { name: "Enable" }).click();

    expect(updateUserStatus).toHaveBeenCalledWith(
      expect.objectContaining({
        body: { accountStatus: "ENABLED" },
        path: { userId: 42 },
      }),
      expect.anything(),
    );
    await expect.element(screen.getByText("Enabled")).toBeVisible();
    await expect.element(screen.getByRole("button", { name: "Disable" })).toBeVisible();
    expect(showNotification).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Account status updated" }),
    );
  });

  it("confirms before disabling users and updates the row in place", async () => {
    const showNotification = vi.spyOn(notifications, "show");
    const updateUserStatus = vi.fn().mockResolvedValue({
      accountStatus: "DISABLED",
      emailAddress: "alice@example.com",
      id: 42,
      role: "USER",
      username: "alice",
    });
    const screen = await render(
      <UserManagementTableTestApp
        updateUserStatus={updateUserStatus}
        users={[
          {
            accountStatus: "ENABLED",
            emailAddress: "alice@example.com",
            id: 42,
            role: "USER",
            username: "alice",
          },
        ]}
      />,
    );

    await screen.getByRole("button", { name: "Disable" }).click();

    expect(updateUserStatus).not.toHaveBeenCalled();
    await expect.element(screen.getByText("Disable alice?")).toBeVisible();

    await screen.getByRole("button", { name: "Confirm disable" }).click();

    expect(updateUserStatus).toHaveBeenCalledWith(
      expect.objectContaining({
        body: { accountStatus: "DISABLED" },
        path: { userId: 42 },
      }),
      expect.anything(),
    );
    await expect.element(screen.getByText("Disabled")).toBeVisible();
    await expect.element(screen.getByRole("button", { name: "Enable" })).toBeVisible();
    expect(showNotification).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Account status updated" }),
    );
  });
});

function UserManagementTableTestApp({
  updateUserStatus,
  users,
}: {
  updateUserStatus?: Parameters<typeof UserManagementTable>[0]["updateUserStatus"];
  users: Array<UserSummary>;
}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(listAdminUsersOptions({ query: { role: "USER" } }).queryKey, users);

  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <Notifications />
        <UserManagementTableFromQuery updateUserStatus={updateUserStatus} />
      </MantineProvider>
    </QueryClientProvider>
  );
}

function UserManagementTableFromQuery({
  updateUserStatus,
}: {
  updateUserStatus?: Parameters<typeof UserManagementTable>[0]["updateUserStatus"];
}) {
  const users = useQuery({
    ...listAdminUsersOptions({ query: { role: "USER" } }),
    staleTime: Infinity,
  });

  return <UserManagementTable updateUserStatus={updateUserStatus} users={users.data ?? []} />;
}

describe("accountStatusLabel", () => {
  it("displays account statuses as human labels", () => {
    expect(accountStatusLabel("ENABLED")).toBe("Enabled");
    expect(accountStatusLabel("DISABLED")).toBe("Disabled");
  });
});
