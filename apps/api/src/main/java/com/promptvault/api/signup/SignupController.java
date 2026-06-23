package com.promptvault.api.signup;

import com.promptvault.contract.api.AuthApi;
import com.promptvault.contract.model.SignupRequest;
import com.promptvault.contract.model.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignupController implements AuthApi {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @Override
    public ResponseEntity<UserSummary> signup(SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signupService.signup(signupRequest));
    }
}
