import type { UserSummary } from "@prompt-vault/api-client";
import {
  getCurrentUserOptions,
  getCurrentUserQueryKey,
  listPolicyKeywordsOptions,
  listPromptCategoriesOptions,
  logoutMutation,
} from "@prompt-vault/api-client";
import {
  ActionIcon,
  AppShell,
  Button,
  Container,
  Group,
  Menu,
  NavLink,
  NavLinkProps,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import {
  type QueryClient,
  useMutation,
  useQueryClient,
  useSuspenseQuery,
} from "@tanstack/react-query";
import {
  createFileRoute,
  Link,
  NavigateOptions,
  Outlet,
  useLocation,
  useNavigate,
  useRouter,
} from "@tanstack/react-router";
import { modals } from "@mantine/modals";

import { requireCurrentUser } from "../features/auth/current-user";
import { CreatePrompt } from "../features/prompts/create-or-edit-prompt";
import { useDisclosure } from "@mantine/hooks";
import { GearIcon } from "@phosphor-icons/react";

type AppNavigationLink = {
  label: string;
  to?: NavigateOptions["href"];
  roles: Array<UserSummary["role"]>;
  children?: AppNavigationLink[];
};

const APP_NAVIGATION_LINKS: Array<AppNavigationLink> = [
  {
    label: "Admin",
    roles: ["ADMIN"],
    children: [
      {
        label: "User management",
        roles: ["ADMIN"],
        to: "/dashboard/admin/users",
      },
      {
        label: "Flagged prompts",
        roles: ["ADMIN"],
        to: "/dashboard/admin/flagged-prompts",
      },
      {
        label: "Prompt categories",
        roles: ["ADMIN"],
        to: "/dashboard/admin/categories",
      },
    ],
  },
  {
    label: "Prompts",
    roles: ["ADMIN", "USER"],
    children: [
      {
        label: "Prompts",
        to: "/dashboard/prompts",
        roles: ["ADMIN", "USER"],
      },
      {
        label: "My submissions",
        to: "/dashboard/prompts/my-submissions",
        roles: ["USER"],
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
  context.queryClient.prefetchQuery(
    listPromptCategoriesOptions({ signal: abortController.signal }),
  );
  context.queryClient.prefetchQuery(listPolicyKeywordsOptions({ signal: abortController.signal }));
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
  const createPromptDisclosure = useDisclosure();
  const navigate = useNavigate();
  const router = useRouter();
  const queryClient = useQueryClient();
  const logout = useMutation(logoutMutation());

  function openKeywordsManager() {
    modals.openContextModal({
      title: "Manage policy keywords",
      modal: "keywords",
      innerProps: {},
      centered: true,
    });
  }

  async function handleLogout() {
    logout.mutate(
      {},
      {
        onSuccess: async () => {
          queryClient.removeQueries({ queryKey: getCurrentUserQueryKey() });
          await router.invalidate();
          await navigate({ to: "/login" });
        },
      },
    );
  }

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
            <Stack gap={0}>
              <Text c="dimmed" fw={700} size="xs" tt="uppercase">
                Prompt Vault
              </Text>
              <Title order={1} size="h3">
                Prompt Vault
              </Title>
            </Stack>
            <Button onClick={createPromptDisclosure[1].open} ml={"auto"}>
              New prompt
            </Button>
            <Menu>
              <Menu.Target>
                <ActionIcon variant="subtle">
                  <GearIcon />
                </ActionIcon>
              </Menu.Target>

              <Menu.Dropdown>
                <Menu.Item onClick={handleLogout}>Logout</Menu.Item>
                {currentUser.role === "ADMIN" ? (
                  <>
                    <Menu.Item onClick={() => openKeywordsManager()}>
                      Manage policy keywords
                    </Menu.Item>
                  </>
                ) : null}
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Container>
      </AppShell.Header>
      <PromptVaultSidebar navigationLinks={navigationLinks} />
      <AppShell.Main>
        <CreatePrompt
          disclosure={createPromptDisclosure}
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
      <NavLink key={link.label} {...props} defaultOpened>
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
