/// <reference types="vite/client" />

import "@mantine/core/styles.css";

import {
  AppShell,
  ColorSchemeScript,
  Container,
  Group,
  MantineProvider,
  Text,
  Title,
  mantineHtmlProps,
} from "@mantine/core";
import type { QueryClient } from "@tanstack/react-query";
import { HeadContent, Outlet, Scripts, createRootRouteWithContext } from "@tanstack/react-router";
import type { ReactNode } from "react";

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient;
}>()({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { title: "Prompt Vault" },
    ],
  }),
  component: RootComponent,
});

function RootComponent() {
  return (
    <html lang="en" {...mantineHtmlProps}>
      <head>
        <ColorSchemeScript />
        <HeadContent />
      </head>
      <body>
        <MantineProvider>
          <PromptVaultShell>
            <Outlet />
          </PromptVaultShell>
        </MantineProvider>
        <Scripts />
      </body>
    </html>
  );
}

function PromptVaultShell({ children }: { children: ReactNode }) {
  return (
    <AppShell header={{ height: 72 }} padding="md">
      <AppShell.Header>
        <Container h="100%" size="md">
          <Group align="center" h="100%" justify="space-between">
            <div>
              <Text c="dimmed" fw={700} size="xs" tt="uppercase">
                Prompt Vault
              </Text>
              <Title order={1} size="h3">
                Prompt Vault
              </Title>
            </div>
          </Group>
        </Container>
      </AppShell.Header>
      <AppShell.Main>
        <Container py="xl" size="md">
          {children}
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}
