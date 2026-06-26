package com.promptvault.api.admin;

import java.util.List;

import com.promptvault.api.prompt.PromptRepository;
import com.promptvault.contract.model.AdminFlaggedPrompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminFlaggedPromptsService {

    private final PromptRepository promptRepository;

    public AdminFlaggedPromptsService(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminFlaggedPrompt> listFlaggedPrompts() {
        return promptRepository.findAllByFlagIsNotNullOrderByCreatedAtDescIdDesc()
            .stream()
            .map(AdminFlaggedPromptMapper::toContract)
            .toList();
    }
}
