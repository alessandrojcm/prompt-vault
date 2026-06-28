package com.promptvault.api.prompt;

import com.promptvault.contract.model.SubmitPromptResponse;

import java.time.ZoneOffset;

public class PromptSubmissionHistoryMapper {
    private PromptSubmissionHistoryMapper() {
    }

    public static SubmitPromptResponse toContract(PromptSubmissionHistoryEntity history) {
        return new SubmitPromptResponse(
                history.getLlmResponse(),
                history.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
