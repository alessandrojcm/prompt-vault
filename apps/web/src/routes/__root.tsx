/// <reference types="vite/client" />

import '@mantine/core/styles.css';

import type { UserSummary } from '@prompt-vault/api-client';
import { getCurrentUserOptions } from '@prompt-vault/api-client';
import {
  AppShell,
  ColorSchemeScript,
  Container,
  Group,
  mantineHtmlProps,
  MantineProvider,
  NavLink,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import {
  createRootRouteWithContext,
  HeadContent,
  Link,
  Outlet,
  Scripts,
  useLocation,
  useRouter,
} from '@tanstack/react-router';
import type { ReactNode } from 'react';
import { TanStackDevtools } from '@tanstack/react-devtools';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

type AppNavigationLink = {
  label: string;
  to: "/dashboard" | "/admin/users";
  roles: Array<UserSummary["role"]>;
};

const APP_NAVIGATION_LINKS: Array<AppNavigationLink> = [
  { label: "Dashboard", roles: ["ADMIN", "USER"], to: "/dashboard" },
  { label: "User management", roles: ["ADMIN"], to: "/admin/users" },
];

type RootLoaderData = {
  navigationLinks: Array<Pick<AppNavigationLink, "label" | "to">>;
};

type RootRouteLoaderContext = {
  context: {
    queryClient: QueryClient;
  };
  abortController: AbortController;
};

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
  loader: loadRootShell,
  component: RootComponent,
});

async function loadRootShell({ context, abortController }: RootRouteLoaderContext) {
  const currentUser = await loadOptionalCurrentUser(context.queryClient, abortController.signal);

  return {
    navigationLinks: currentUser === null ? [] : navigationLinksFor(currentUser),
  } satisfies RootLoaderData;
}

async function loadOptionalCurrentUser(queryClient: QueryClient, signal: AbortSignal) {
  try {
    return (await queryClient.ensureQueryData(getCurrentUserOptions({ signal }))) as UserSummary;
  } catch {
    return null;
  }
}

function navigationLinksFor(currentUser: UserSummary) {
  return APP_NAVIGATION_LINKS.filter((link) => link.roles.includes(currentUser.role)).map(
    ({ label, to }) => ({ label, to }),
  );
}

function RootComponent() {
  const { navigationLinks } = Route.useLoaderData();

  return (
    <html lang="en" {...mantineHtmlProps}>
      <head>
        <ColorSchemeScript />
        <HeadContent />
      </head>
      <body>
        <MantineProvider>
          <PromptVaultShell navigationLinks={navigationLinks}>
            <Outlet />
          </PromptVaultShell>
        </MantineProvider>
        <Scripts />
      </body>
    </html>
  );
}

function PromptVaultShell({
  children,
  navigationLinks,
}: {
  children: ReactNode;
  navigationLinks: RootLoaderData["navigationLinks"];
}) {
  const hasNavigationLinks = navigationLinks.length > 0;
  const router = useRouter();
  const queryClient = useQueryClient();

  return (
    <>
      <AppShell
        header={{ height: 72 }}
        navbar={{
          breakpoint: "sm",
          collapsed: { desktop: !hasNavigationLinks, mobile: true },
          width: 260,
        }}
        padding="md"
      >
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
        {hasNavigationLinks ? <PromptVaultSidebar navigationLinks={navigationLinks} /> : null}
        <AppShell.Main>
          <Container py="xl" size="md">
            {children}
          </Container>
        </AppShell.Main>
      </AppShell>
      {import.meta.env.DEV && (
        <TanStackDevtools
          plugins={[
            
            {
              name: "TanStack Query",
              render: <ReactQueryDevtools client={queryClient} />,
            },
            {
              name: "TanStack Router",
              render: <TanStackRouterDevtools router={router} />,
            },
          ]}
        />
      )}
    </>
  );
}

function PromptVaultSidebar({
  navigationLinks,
}: {
  navigationLinks: RootLoaderData["navigationLinks"];
}) {
  const location = useLocation();

  return (
    <AppShell.Navbar p="md">
      <Stack gap="xs">
        <Text c="dimmed" fw={700} size="xs" tt="uppercase">
          Sections
        </Text>
        {navigationLinks.map((link) => (
          <NavLink
            key={link.to}
            active={isActiveNavigationLink(location.pathname, link.to)}
            component={Link}
            label={link.label}
            to={link.to}
          />
        ))}
      </Stack>
    </AppShell.Navbar>
  );
}

function isActiveNavigationLink(pathname: string, linkPathname: string) {
  return pathname === linkPathname || pathname.startsWith(`${linkPathname}/`);
}
