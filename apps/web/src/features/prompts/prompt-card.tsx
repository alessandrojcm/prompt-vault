import {
  ActionIcon,
  Button,
  ButtonGroup,
  Card,
  CardProps,
  Chip,
  DataList,
  Group,
  Pill,
  Popover,
  Stack,
  Text,
} from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import {
  deletePromptMutation,
  getCurrentUserOptions,
  listMyPromptsOptions,
  listPromptCategoriesOptions,
  listPromptsOptions,
  Prompt,
  updatePromptVisibilityMutation,
} from '@prompt-vault/api-client';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { TrashIcon } from '@phosphor-icons/react';
import { PencilIcon } from '@phosphor-icons/react/dist/ssr';
import { useDisclosure } from '@mantine/hooks';
import { CreatePrompt } from './create-or-edit-prompt';
import { Link } from '@tanstack/react-router';
import classes from './prompt-card.module.css';

type Props = Prompt & {
  categoryLabel: string;
  enableEditing?: boolean;
  enableSubmission?: boolean;
  onClick?: () => void;
};

export function PromptCard({
  categoryLabel,
  enableEditing = true,
  enableSubmission = true,
  onClick,
  ...props
}: Props) {
  const currentUser = useSuspenseQuery(getCurrentUserOptions());
  const client = useQueryClient();
  const isMyPrompt = props.ownerUserId === currentUser.data.id;
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
      client.invalidateQueries(listPromptsOptions({ query: { visibility: ["PUBLIC"] } }));
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
      client.invalidateQueries(listPromptsOptions({ query: { visibility: ["PUBLIC"] } }));
    },
  });

  const clickProps: CardProps = onClick
    ? {
        // @ts-ignore
        onClick,
        component: "button",
        className: classes["hover-on-float"],
      }
    : {};
  return (
    <Card shadow="md" withBorder w={"min-content"} {...clickProps}>
      <Card.Section inheritPadding p="xs" withBorder>
        <DataList>
          <DataList.Item>
            <DataList.ItemLabel>Title</DataList.ItemLabel>
            <DataList.ItemValue>
              <Group wrap={"nowrap"}>
                <Text>{props.title}</Text>
                {isMyPrompt && enableEditing ? (
                  <>
                    <ActionIcon
                      variant="subtle"
                      aria-label="Edit"
                      onClick={() => disclosure[1].open()}
                    >
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
                  </>
                ) : null}
              </Group>
            </DataList.ItemValue>
          </DataList.Item>
          <DataList.Item>
            <DataList.ItemLabel>Category</DataList.ItemLabel>
            <DataList.ItemValue>
              <Pill>{categoryLabel}</Pill>
            </DataList.ItemValue>
          </DataList.Item>
          {isMyPrompt && enableEditing ? (
            <DataList.Item>
              <DataList.ItemLabel>Visibility</DataList.ItemLabel>
              <DataList.ItemValue>
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
              </DataList.ItemValue>
            </DataList.Item>
          ) : null}
        </DataList>
      </Card.Section>
      <Card.Section inheritPadding p="xs" withBorder>
        <Stack align={"center"}>
          <Text fw={500}>Content</Text>
          <Text size={"sm"}>{props.text}</Text>
        </Stack>
      </Card.Section>
      {enableSubmission ? (
        <Card.Section inheritPadding p="xs" withBorder>
          <Group justify={"center"}>
            <Button
              size="sm"
              variant="outline"
              component={Link}
              to="/dashboard/prompts/$promptId/submit"
              params={{ promptId: props.id }}
            >
              Submit to model
            </Button>
          </Group>
        </Card.Section>
      ) : null}
      <CreatePrompt
        categories={categories.data}
        currentUser={currentUser.data}
        prompt={props as Prompt}
        disclosure={disclosure}
      />
    </Card>
  );
}
