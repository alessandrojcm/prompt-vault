import { Anchor, Card, Text, Title } from '@mantine/core';
import { createFileRoute, Link } from '@tanstack/react-router';

export const Route = createFileRoute("/signup/success")({
  component: SignupSuccessPage,
});

function SignupSuccessPage() {
  return (
    <Card maw={520} mx="auto" padding="xl" radius="md" shadow="sm" withBorder>
      <Text c="dimmed" fw={700} size="xs" tt="uppercase">
        Prompt Vault
      </Text>
      <Title order={2}>Signup complete</Title>
      <Text mt="md">Your user has been created. You can now log in.</Text>
      <Anchor component={Link} mt="md" to="/login">
        Log in
      </Anchor>
    </Card>
  );
}
