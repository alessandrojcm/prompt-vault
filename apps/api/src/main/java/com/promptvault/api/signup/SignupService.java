package com.promptvault.api.signup;

import com.promptvault.api.user.*;
import com.promptvault.contract.model.SignupRequest;
import com.promptvault.contract.model.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SignupService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserSummary signup(SignupRequest request) {
        String username = trimToEmpty(request.getUsername());
        String emailAddress = trimToEmpty(request.getEmailAddress());
        String password = nullToEmpty(request.getPassword());

        List<FieldValidationError> fieldErrors = validate(username, emailAddress, password);
        String usernameNormalized = normalize(username);
        String emailAddressNormalized = normalize(emailAddress);

        if (userRepository.existsByUsernameNormalized(usernameNormalized)) {
            fieldErrors.add(new FieldValidationError("username", "Username is already taken."));
        }

        if (userRepository.existsByEmailAddressNormalized(emailAddressNormalized)) {
            fieldErrors.add(new FieldValidationError("emailAddress", "Email Address is already taken."));
        }

        if (!fieldErrors.isEmpty()) {
            throw new SignupValidationException(fieldErrors);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(usernameNormalized);
        user.setEmailAddress(emailAddress);
        user.setEmailAddressNormalized(emailAddressNormalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ENABLED);

        UserEntity createdUser = userRepository.save(user);

        return UserSummaryMapper.toSummary(createdUser);
    }

    private List<FieldValidationError> validate(String username, String emailAddress, String password) {
        List<FieldValidationError> fieldErrors = new ArrayList<>();

        if (username.isBlank()) {
            fieldErrors.add(new FieldValidationError("username", "Username is required."));
        } else {
            if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
                fieldErrors.add(new FieldValidationError("username", "Username must be 3 to 30 characters long."));
            }

            if (!USERNAME_PATTERN.matcher(username).matches()) {
                fieldErrors.add(new FieldValidationError("username", "Username may only contain letters, numbers, dots, hyphens, and underscores."));
            }
        }

        if (emailAddress.isBlank()) {
            fieldErrors.add(new FieldValidationError("emailAddress", "Email Address is required."));
        } else if (!emailAddress.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            fieldErrors.add(new FieldValidationError("emailAddress", "Email Address must be valid."));
        }

        if (password.isEmpty() || password.isBlank()) {
            fieldErrors.add(new FieldValidationError("password", "Password is required."));
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            fieldErrors.add(new FieldValidationError("password", "Password must be at least 8 characters long."));
        }

        return fieldErrors;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
