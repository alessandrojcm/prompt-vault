package com.promptvault.api.admin;

import com.promptvault.api.prompt.PromptEntity;
import com.promptvault.api.prompt.PromptFlagKeywordSnapshotEntity;
import com.promptvault.contract.model.AdminFlaggedPrompt;

import java.time.ZoneOffset;

final class AdminFlaggedPromptMapper {

    private AdminFlaggedPromptMapper() {
    }

    static AdminFlaggedPrompt toContract(PromptEntity prompt) {
        return new AdminFlaggedPrompt(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getOwner().getUsername(),
                prompt.getCategory().getLabel(),
                prompt.getFlag().getKeywordSnapshots()
                        .stream()
                        .map(PromptFlagKeywordSnapshotEntity::getKeywordText)
                        .toList(),
                prompt.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
