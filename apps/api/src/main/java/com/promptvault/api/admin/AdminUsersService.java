package com.promptvault.api.admin;

import java.util.List;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.AccountStatus;
import com.promptvault.api.user.Role;
import com.promptvault.api.user.UserEntity;
import com.promptvault.api.user.UserRepository;
import com.promptvault.api.user.UserSummaryMapper;
import com.promptvault.contract.model.UserRole;
import com.promptvault.contract.model.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUsersService {

    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    public AdminUsersService(UserRepository userRepository, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers(UserRole role) {
        if (role == null) {
            return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(UserSummaryMapper::toSummary)
                .toList();
        }

        return userRepository.findAllByRoleOrderByUsernameAsc(Role.valueOf(role.getValue()))
            .stream()
            .map(UserSummaryMapper::toSummary)
            .toList();
    }

    @Transactional
    public UserSummary updateUserStatus(Long userId, com.promptvault.contract.model.AccountStatus requestedStatus) {
        UserEntity target = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (target.getRole() == Role.ADMIN || target.getId().equals(currentUserId())) {
            throw new AccessDeniedException("User account status cannot be managed by this operation");
        }

        AccountStatus accountStatus = AccountStatus.valueOf(requestedStatus.getValue());
        target.setAccountStatus(accountStatus);

        if (accountStatus == AccountStatus.DISABLED) {
            revokeSessions(target.getId());
        }

        return UserSummaryMapper.toSummary(target);
    }

    private void revokeSessions(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
            .filter(PromptVaultUserDetails.class::isInstance)
            .map(PromptVaultUserDetails.class::cast)
            .filter(userDetails -> userDetails.getUser().getId().equals(userId))
            .flatMap(userDetails -> sessionRegistry.getAllSessions(userDetails, false).stream())
            .forEach(SessionInformation::expireNow);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser().getId();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
