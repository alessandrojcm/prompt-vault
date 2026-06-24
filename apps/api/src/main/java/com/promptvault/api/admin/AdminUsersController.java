package com.promptvault.api.admin;

import java.util.List;

import com.promptvault.contract.api.AdminUsersApi;
import com.promptvault.contract.model.UserRole;
import com.promptvault.contract.model.UserSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUsersController implements AdminUsersApi {

    private final AdminUsersService adminUsersService;

    public AdminUsersController(AdminUsersService adminUsersService) {
        this.adminUsersService = adminUsersService;
    }

    @Override
    public ResponseEntity<List<UserSummary>> listAdminUsers(@Nullable UserRole role) {
        return ResponseEntity.ok(adminUsersService.listUsers(role));
    }
}
