package com.promptvault.api.prompt;

import java.time.ZoneOffset;

import com.promptvault.contract.model.Prompt;
import com.promptvault.contract.model.PublicPrompt;

public final class PromptMapper {

    private PromptMapper() {
    }

    public static Prompt toContract(PromptEntity prompt) {
        return new Prompt(
            prompt.getId(),
            prompt.getTitle(),
            prompt.getText(),
            com.promptvault.contract.model.PromptVisibility.fromValue(prompt.getVisibility().name()),
            prompt.getCategory().getId(),
            prompt.getOwner().getId(),
            prompt.getCreatedAt().atOffset(ZoneOffset.UTC),
            prompt.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    public static PublicPrompt toPublicContract(PromptEntity prompt) {
        return new PublicPrompt(
            prompt.getId(),
            prompt.getTitle(),
            prompt.getText(),
            com.promptvault.contract.model.PromptVisibility.fromValue(prompt.getVisibility().name()),
            prompt.getCategory().getId(),
            prompt.getOwner().getUsername(),
            prompt.getCreatedAt().atOffset(ZoneOffset.UTC),
            prompt.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
