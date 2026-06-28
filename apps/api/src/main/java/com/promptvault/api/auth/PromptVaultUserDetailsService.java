package com.promptvault.api.auth;

import com.promptvault.api.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PromptVaultUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public PromptVaultUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String usernameNormalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameNormalized(usernameNormalized)
                .map(PromptVaultUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
