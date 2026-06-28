import { ActionIcon, Button, ButtonGroup, Card, Chip, Group, Pill, Popover, Stack, Text, } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import {
  deletePromptMutation,
  getCurrentUserOptions,
  listMyPromptsOptions,
  listPromptCategoriesOptions,
  Prompt,
  PublicPrompt,
  updatePromptVisibilityMutation,
} from '@prompt-vault/api-client';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { TrashIcon } from '@phosphor-icons/react';
import { PencilIcon } from '@phosphor-icons/react/dist/ssr';
import { useDisclosure } from '@mantine/hooks';
import { CreatePrompt } from './create-or-edit-prompt';
import { Link } from '@tanstack/react-router';

type Props = (Prompt | PublicPrompt) & {
  categoryLabel: string;
};

export function PromptCard({ categoryLabel, ...props }: Props) {
  const currentUser = useSuspenseQuery(getCurrentUserOptions());
  const client = useQueryClient();
  const isMyPrompt = "ownerUserId" in props && props.ownerUserId === currentUser.data.id;
  const [opened, setOpened] = useState(false);
  const disclosure = useDisclosure();
  const categories = useSuspenseQuery(listPromptCategoriesOptions());

  const deletePrompt = useMutation({
    ...deletePromptMutation(),
    onError: () => {
      showNotification({
        message: "There was a problem deleting your prompt",
        color: "red",
        position: "top-right",
      });
    },
    onSuccess: () => {
      showNotification({
        message: "Prompt delete successfully",
        position: "top-right",
      });
      client.invalidateQueries(listMyPromptsOptions({ path: { userId: currentUser.data.id } }));
    },
  });

  const changeVisibility = useMutation({
    ...updatePromptVisibilityMutation(),
    onError: () => {
      showNotification({
        message: "There was a problem changing your prompt's visibility",
        color: "red",
        position: "top-right",
      });
    },
    onSuccess: () => {
      showNotification({
        message: "Prompt's visibility updated successfully",
        position: "top-right",
      });
      client.invalidateQueries(listMyPromptsOptions({ path: { userId: currentUser.data.id } }));
    },
  });

  return (
    <Card shadow="md" padding="xl">
      <Stack gap={"md"} h={"100%"} w={"100%"}>
        <Group w={"100%"}>
          <Text fw={500} size="lg">
            {props.title}
          </Text>

          {isMyPrompt ? (
            <Group ml={"auto"} gap={"xs"}>
              <Chip
                mr="auto"
                color="blue"
                aria-label={`Make ${props.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC"}`}
                style={{ textTransform: "capitalize" }}
                checked={props.visibility === "PUBLIC"}
                disabled={changeVisibility.isPending}
                onChange={() =>
                  changeVisibility.mutate({
                    path: {
                      promptId: props.id,
                    },
                    body: {
                      visibility: props.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC",
                    },
                  })
                }
              >
                {props.visibility.toLowerCase()}
              </Chip>
              <ActionIcon variant="subtle" aria-label="Edit" onClick={() => disclosure[1].open()}>
                <PencilIcon />
              </ActionIcon>
              <Popover
                opened={opened}
                width={300}
                trapFocus
                position="bottom"
                withArrow
                shadow="md"
                onDismiss={() => setOpened(false)}
              >
                <Popover.Target>
                  <ActionIcon
                    aria-label="Delete"
                    color="red"
                    variant="subtle"
                    onClick={() => setOpened(true)}
                  >
                    <TrashIcon />
                  </ActionIcon>
                </Popover.Target>
                <Popover.Dropdown>
                  <Text size="xs">Are you sure you want to deletet this prompt?</Text>
                  <ButtonGroup>
                    <Button variant="filled" onClick={() => setOpened(false)}>
                      No
                    </Button>
                    <Button
                      color="red"
                      variant="outline"
                      onClick={() => {
                        deletePrompt.mutate({
                          path: { promptId: props.id },
                        });
                        setOpened(false);
                      }}
                    >
                      Yes
                    </Button>
                  </ButtonGroup>
                </Popover.Dropdown>
              </Popover>
            </Group>
          ) : null}
        </Group>
        <Text>{props.text}</Text>
        <Group mt={"auto"}>
          <Pill>{categoryLabel}</Pill>
          <Button
            size="sm"
            ml="auto"
            variant="outline"
            component={Link}
            to="/dashboard/prompts/$promptId/submit"
            params={{ promptId: props.id }}
          >
            Submit to model
          </Button>
        </Group>
      </Stack>
      <CreatePrompt
        categories={categories.data}
        currentUser={currentUser.data}
        prompt={props as Prompt}
        disclosure={disclosure}
      />
    </Card>
  );
}
