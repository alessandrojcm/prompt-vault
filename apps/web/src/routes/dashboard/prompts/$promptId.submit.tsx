import { Button, Card, Container, Loader, ScrollArea, Stack, Text, Title } from '@mantine/core';
import { getPromptOptions, submitPromptRequestMutation } from '@prompt-vault/api-client';
import { useMutation } from '@tanstack/react-query';
import { createFileRoute, redirect } from '@tanstack/react-router';
import { useEffect } from 'react';
import { fetchServerSentEvents, useChat } from '@tanstack/ai-react';
import { showNotification } from '@mantine/notifications';

export const Route = createFileRoute("/dashboard/prompts/$promptId/submit")({
  component: RouteComponent,
  beforeLoad: async ({ params, context }) => {
    try {
      return await context.queryClient.ensureQueryData(
        getPromptOptions({ path: { promptId: Number(params.promptId) } }),
      );
    } catch (err) {
      console.error(err);
      redirect({
        statusCode: 404,
      });
    }
  },
  loader: async ({ context, params }) => {
    const data = await context.queryClient.ensureQueryData(
      getPromptOptions({ path: { promptId: Number(params.promptId) } }),
    );

    return {
      prompt: data,
      userId: context.id,
    };
  },
  notFoundComponent: () => (
    <Container>
      <Title>Prompt not found</Title>
    </Container>
  ),
});

function RouteComponent() {
  const params = Route.useParams();
  const { prompt, userId } = Route.useLoaderData();
  const saveSubmission = useMutation({
    ...submitPromptRequestMutation(),
    onError: () => {
      showNotification({
        color: "red",
        message: "There was an error saving your submission",
      });
    },
  });
  const { messages, sendMessage, reload, isLoading } = useChat({
    connection: fetchServerSentEvents("/chat"),
    onFinish: () => {
      messages.filter((r) => r.role === "assistant");
      saveSubmission.mutate({
        path: {
          promptId: Number(params.promptId),
          userId,
        },
        body: {
          response: messages
            .filter((r) => r.role === "assistant")
            .flatMap((p) => {
              const messages = p.parts.filter((p) => p.type === "text");
              return messages.map((m) => m.content);
            })
            .join("\n"),
        },
      });
    },
  });

  useEffect(() => {
    sendMessage(prompt.text);
  }, [prompt]);

  return (
    <Card aria-label="Submission chat" padding="xl" radius="md" shadow="sm" withBorder>
      <Title order={2} mb={1}>
        Submission chat for {prompt.title}
      </Title>
      <Container size={"xl"}>
        <Stack>
          <ScrollArea h={500}>
            {messages.map((message) => (
              <Container
                w={"max-content"}
                bg={message.role === "assistant" ? "blue" : "gray"}
                bdrs={"md"}
                mr={message.role === "assistant" ? "auto" : "0px"}
                ml={message.role === "user" ? "auto" : "0px"}
                key={message.id}
              >
                <Text key={message.id} mb={4}>
                  <Text fw={250}>{message.role === "assistant" ? "Assistant" : "You"}</Text>
                  {message.role === "assistant" && isLoading ? (
                    <Loader size={"sm"} color={"dark"} />
                  ) : null}
                  <div>
                    {message.parts.map((part, idx) => {
                      if (part.type === "text") {
                        return <Text key={idx}>{part.content}</Text>;
                      }
                      return null;
                    })}
                  </div>
                </Text>
              </Container>
            ))}
          </ScrollArea>
        </Stack>
      </Container>
      <Button disabled={isLoading} variant="outline" mr="auto" mt={4} onClick={() => reload()}>
        Restart
      </Button>
    </Card>
  );
}
