import type { UserSummary } from "@prompt-vault/api-client";
import { getCurrentUserOptions, listPromptCategoriesOptions } from "@prompt-vault/api-client";
import {
  AppShell,
  Button,
  Container,
  Group,
  NavLink,
  NavLinkProps,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import { useSuspenseQuery, type QueryClient } from "@tanstack/react-query";
import {
  createFileRoute,
  Link,
  NavigateOptions,
  Outlet,
  useLocation,
} from "@tanstack/react-router";

import { requireCurrentUser } from "../features/auth/current-user";
import { CreatePrompt } from "../features/prompts/create-or-edit-prompt";
import { useDisclosure } from "@mantine/hooks";

type AppNavigationLink = {
  label: string;
  to?: NavigateOptions["href"];
  roles: Array<UserSummary["role"]>;
  children?: AppNavigationLink[];
};

const APP_NAVIGATION_LINKS: Array<AppNavigationLink> = [
  { label: "Dashboard", roles: ["ADMIN", "USER"], to: "/dashboard" },
  {
    label: "User management",
    roles: ["ADMIN"],
    to: "/dashboard/admin/users",
  },
  {
    label: "Prompts",
    roles: ["ADMIN", "USER"],
    children: [
      {
        label: "My Prompts",
        to: "/dashboard/my-prompts",
        roles: ["ADMIN", "USER"],
      },
      {
        label: "Prompts",
        to: "/dashboard/prompts",
        roles: ["ADMIN", "USER"],
      },
    ],
  },
];

type DashboardLoaderData = {
  navigationLinks: Array<Pick<AppNavigationLink, "label" | "to" | "children">>;
  currentUser: UserSummary;
};

type DashboardRouteLoaderContext = {
  context: {
    queryClient: QueryClient;
  };
  abortController: AbortController;
};

export const Route = createFileRoute("/dashboard")({
  beforeLoad: requireCurrentUser,
  loader: loadDashboardShell,
  component: DashboardLayout,
});

async function loadDashboardShell({ context, abortController }: DashboardRouteLoaderContext) {
  const currentUser = (await context.queryClient.ensureQueryData(
    getCurrentUserOptions({ signal: abortController.signal }),
  )) as UserSummary;
  context.queryClient.ensureQueryData(
    listPromptCategoriesOptions({ signal: abortController.signal }),
  );
  return {
    navigationLinks: navigationLinksFor(currentUser),
    currentUser,
  } satisfies DashboardLoaderData;
}

function navigationLinksFor(currentUser: UserSummary) {
  return APP_NAVIGATION_LINKS.filter((link) => link.roles.includes(currentUser.role)).map(
    ({ label, to, children }) => ({ label, to, children }),
  );
}

function DashboardLayout() {
  const { navigationLinks, currentUser } = Route.useLoaderData();
  const { data: promptCategories = [] } = useSuspenseQuery(listPromptCategoriesOptions());
  const disclosure = useDisclosure();

  return (
    <AppShell
      header={{ height: 72 }}
      navbar={{
        breakpoint: "sm",
        collapsed: { desktop: false, mobile: true },
        width: 260,
      }}
      padding="md"
    >
      <AppShell.Header>
        <Container h="100%" size="xl">
          <Group align="center" h="100%" justify="space-between">
            <div>
              <Text c="dimmed" fw={700} size="xs" tt="uppercase">
                Prompt Vault
              </Text>
              <Title order={1} size="h3">
                Prompt Vault
              </Title>
            </div>
            <Button onClick={disclosure[1].open} ml={"auto"}>
              New prompt
            </Button>
          </Group>
        </Container>
      </AppShell.Header>
      <PromptVaultSidebar navigationLinks={navigationLinks} />
      <AppShell.Main>
        <CreatePrompt
          disclosure={disclosure}
          categories={promptCategories}
          currentUser={currentUser}
        />

        <Container py="xl" size="xl">
          <Outlet />
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}

function PromptVaultSidebar({
  navigationLinks,
}: {
  navigationLinks: DashboardLoaderData["navigationLinks"];
}) {
  return (
    <AppShell.Navbar p="md">
      <Stack gap="xs">
        <Text c="dimmed" fw={700} size="xs" tt="uppercase">
          Sections
        </Text>
        {renderNavigationLinks(navigationLinks)}
      </Stack>
    </AppShell.Navbar>
  );
}

function renderNavigationLinks(links: DashboardLoaderData["navigationLinks"]) {
  const location = useLocation();
  return links.map((link) => {
    const props: NavLinkProps = {
      ...(link?.to && {
        component: Link,
        to: link.to,
        active: isActiveNavigationLink(location.pathname, link.to),
      }),
      label: link.label,
    };
    return link?.children ? (
      <NavLink key={link.label} {...props}>
        {renderNavigationLinks(link.children)}
      </NavLink>
    ) : (
      <NavLink key={link.label} {...props} />
    );
  });
}

function isActiveNavigationLink(pathname: string, linkPathname: string) {
  return pathname === linkPathname || pathname.startsWith(`${linkPathname}/`);
}
