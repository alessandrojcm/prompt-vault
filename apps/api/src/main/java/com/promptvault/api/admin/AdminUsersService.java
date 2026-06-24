package com.promptvault.api.admin;

import java.util.List;

import com.promptvault.api.user.Role;
import com.promptvault.api.user.UserRepository;
import com.promptvault.api.user.UserSummaryMapper;
import com.promptvault.contract.model.UserRole;
import com.promptvault.contract.model.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUsersService {

    private final UserRepository userRepository;

    public AdminUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
