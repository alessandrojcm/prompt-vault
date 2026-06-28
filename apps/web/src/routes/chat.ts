import { chat, toServerSentEventsResponse } from "@tanstack/ai";
import { openRouterText } from "@tanstack/ai-openrouter";
import { createFileRoute } from "@tanstack/react-router";

const SYSTEM_PROMPT = `
  "Your an LLM in Prompt Vault, a prompt management application.
  Users can create a share prompts in this applications, and submit to an LLM to observe the response.
  Submission is one shot only, users cannot reply to you, and you will only receive a single prompt.
  You will reply to the best of your ability and will not ask follow up questions, do not use markdown.",`;

export const Route = createFileRoute("/chat")({
  server: {
    handlers: {
      POST: async ({ request }) => {
        if (!process.env.OPENROUTER_API_KEY) {
          return new Response(
            JSON.stringify({
              error: "OPEN_ROUTER not configured",
            }),
            {
              status: 500,
              headers: { "Content-Type": "application/json" },
            },
          );
        }

        const body = await request.json();

        try {
          const stream = chat({
            adapter: openRouterText("nvidia/nemotron-3-nano-30b-a3b:free"),
            systemPrompts: [SYSTEM_PROMPT],
            messages: body.messages,
          });

          return toServerSentEventsResponse(stream);
        } catch (error) {
          return new Response(
            JSON.stringify({
              error: error instanceof Error ? error.message : "An error occurred",
            }),
            {
              status: 500,
              headers: { "Content-Type": "application/json" },
            },
          );
        }
      },
    },
  },
});
