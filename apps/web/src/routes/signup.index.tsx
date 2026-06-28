import { createFileRoute } from '@tanstack/react-router';

import { SignupScreen } from '../features/signup/signup-screen';

export const Route = createFileRoute("/signup/")({
  component: SignupPage,
});

function SignupPage() {
  return <SignupScreen />;
}
