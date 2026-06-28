package com.promptvault.api.admin;

import com.promptvault.contract.api.AdminPromptsApi;
import com.promptvault.contract.model.AdminFlaggedPrompt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminFlaggedPromptsController implements AdminPromptsApi {

    private final AdminFlaggedPromptsService adminFlaggedPromptsService;

    public AdminFlaggedPromptsController(AdminFlaggedPromptsService adminFlaggedPromptsService) {
        this.adminFlaggedPromptsService = adminFlaggedPromptsService;
    }

    @Override
    public ResponseEntity<List<AdminFlaggedPrompt>> listAdminFlaggedPrompts() {
        return ResponseEntity.ok(adminFlaggedPromptsService.listFlaggedPrompts());
    }
}
