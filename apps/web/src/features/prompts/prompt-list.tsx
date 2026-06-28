import { Prompt, PublicPrompt } from '@prompt-vault/api-client';
import { PromptCard } from './prompt-card';
import { SimpleGrid, Title } from '@mantine/core';

type Props = {
  categoriesMap: Map<number, string>;
  prompts: (Prompt | PublicPrompt)[];
  title: string;
};

export function PromptList(props: Props) {
  const { categoriesMap, prompts, title } = props;

  return (
    <>
      <Title mb={4}>{title}</Title>
      <SimpleGrid cols={3}>
        {prompts.map((p) => (
          <PromptCard categoryLabel={categoriesMap.get(p.categoryId)!} key={p.id} {...p} />
        ))}
      </SimpleGrid>
    </>
  );
}
