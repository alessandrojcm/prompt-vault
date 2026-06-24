package com.promptvault.api.auth;

import com.promptvault.api.user.UserEntity;
import com.promptvault.api.user.UserSummaryMapper;
import com.promptvault.contract.model.LoginRequest;
import com.promptvault.contract.model.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public LoginService(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public UserSummary login(LoginRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword())
            );
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);
            return UserSummaryMapper.toSummary(userFrom(authentication));
        } catch (DisabledException exception) {
            throw new DisabledAccountException();
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private UserEntity userFrom(Authentication authentication) {
        if (authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
