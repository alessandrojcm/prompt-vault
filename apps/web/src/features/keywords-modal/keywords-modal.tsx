import { Pill, PillsInput, Text } from "@mantine/core";
import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import {
  createPolicyKeywordMutation,
  deletePolicyKeywordMutation,
  listPolicyKeywordsOptions,
  listPolicyKeywordsQueryKey,
  ListPolicyKeywordsResponse,
} from "@prompt-vault/api-client";
import { getHotkeyHandler } from "@mantine/hooks";
import { useState } from "react";

export function KeywordsModal() {
  const client = useQueryClient();
  const keywords = useSuspenseQuery(listPolicyKeywordsOptions());
  const deleteKeyword = useMutation({
    ...deletePolicyKeywordMutation(),
    onSuccess: () => {
      keywords.refetch();
    },
  });
  const addKeyword = useMutation({
    ...createPolicyKeywordMutation(),
    onSuccess: (newKeyword) => {
      client.setQueryData<ListPolicyKeywordsResponse>(listPolicyKeywordsQueryKey(), (data) => [
        ...(data ?? []),
        newKeyword,
      ]);
      keywords.refetch();
      setNewKeyword("");
    },
  });
  const isLoading = [keywords.isLoading, deleteKeyword.isPending, addKeyword.isPending].some(
    Boolean,
  );
  const [newKeyword, setNewKeyword] = useState("");
  return (
    <>
      <Text size={"sm"}></Text>
      <PillsInput label="Enter items" loading={isLoading}>
        <Pill.Group>
          {keywords.data.map((c) => (
            <Pill
              withRemoveButton
              style={{
                textTransform: "capitalize",
              }}
              onRemove={() =>
                deleteKeyword.mutate({
                  path: {
                    keywordId: c.id,
                  },
                })
              }
              key={c.id}
            >
              {c.keyword}
            </Pill>
          ))}
          <PillsInput.Field
            placeholder="Enter new keyword"
            value={newKeyword}
            onChange={(event) => setNewKeyword(event.currentTarget.value)}
            onKeyUp={getHotkeyHandler([
              [
                "Enter",
                () =>
                  addKeyword.mutate({
                    body: {
                      keyword: newKeyword,
                    },
                  }),
              ],
            ])}
          />
        </Pill.Group>
      </PillsInput>
    </>
  );
}
