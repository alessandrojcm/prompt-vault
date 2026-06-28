import { Card, Pill, Stack, Text } from "@mantine/core";
import { Prompt, PublicPrompt } from "@prompt-vault/api-client";

export function PromptCard(props: Prompt | (PublicPrompt & { categoryLabel: string })) {
  return (
    <Card shadow="md" padding="xl">
      <Stack gap={"md"}>
        <Text fw={500} size="lg">
          {props.title}
        </Text>
        <Text>{props.text}</Text>
        <Pill mr="auto">{props.categoryLabel}</Pill>
      </Stack>
    </Card>
  );
}
