import { MantineProvider } from "@mantine/core";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-react";

import { UserManagementTable, accountStatusLabel } from "./user-management";

describe("UserManagementTable", () => {
  it("shows normal-user management columns and hides internal ids and roles", async () => {
    const screen = await render(
      <MantineProvider>
        <UserManagementTable
          users={[
            {
              accountStatus: "ENABLED",
              emailAddress: "alice@example.com",
              id: 42,
              role: "USER",
              username: "alice",
            },
          ]}
        />
      </MantineProvider>,
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
    await expect.element(screen.getByRole("button", { name: "Manage" })).toBeDisabled();
    await expect.element(screen.getByText("42")).not.toBeInTheDocument();
    await expect.element(screen.getByText("USER", { exact: true })).not.toBeInTheDocument();
  });
});

describe("accountStatusLabel", () => {
  it("displays account statuses as human labels", () => {
    expect(accountStatusLabel("ENABLED")).toBe("Enabled");
    expect(accountStatusLabel("DISABLED")).toBe("Disabled");
  });
});
