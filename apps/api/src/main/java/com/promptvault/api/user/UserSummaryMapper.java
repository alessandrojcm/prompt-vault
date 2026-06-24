package com.promptvault.api.user;

import com.promptvault.contract.model.UserSummary;

public final class UserSummaryMapper {

    private UserSummaryMapper() {
    }

    public static UserSummary toSummary(UserEntity user) {
        UserSummary summary = new UserSummary();
        summary.setId(user.getId());
        summary.setUsername(user.getUsername());
        summary.setEmailAddress(user.getEmailAddress());
        summary.setRole(com.promptvault.contract.model.UserRole.fromValue(user.getRole().name()));
        summary.setAccountStatus(com.promptvault.contract.model.AccountStatus.fromValue(user.getAccountStatus().name()));
        return summary;
    }
}
