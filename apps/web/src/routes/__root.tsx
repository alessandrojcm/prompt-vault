/// <reference types="vite/client" />

import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';

import { ColorSchemeScript, mantineHtmlProps, MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import { createRootRouteWithContext, HeadContent, Outlet, Scripts, useRouter, } from '@tanstack/react-router';
import { TanStackDevtools } from '@tanstack/react-devtools';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { formDevtoolsPlugin } from '@tanstack/react-form-devtools';
import { ModalsProvider } from '@mantine/modals';
import { KeywordsModal } from '../features/keywords-modal/keywords-modal';

const contextModals = {
  keywords: KeywordsModal,
};
declare module "@mantine/modals" {
  export interface MantineModalsOverride {
    modals: typeof contextModals;
  }
}

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
          <Notifications />
          <ModalsProvider modals={contextModals}>
            <Outlet />
            <PromptVaultDevtools />
          </ModalsProvider>
        </MantineProvider>
        <Scripts />
      </body>
    </html>
  );
}

function PromptVaultDevtools() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return (
    import.meta.env.DEV && (
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
          formDevtoolsPlugin(),
        ]}
      />
    )
  );
}
