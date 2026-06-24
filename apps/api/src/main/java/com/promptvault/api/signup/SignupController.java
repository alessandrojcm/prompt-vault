package com.promptvault.api.signup;

import com.promptvault.api.auth.LoginService;
import com.promptvault.contract.api.AuthApi;
import com.promptvault.contract.model.LoginRequest;
import com.promptvault.contract.model.SignupRequest;
import com.promptvault.contract.model.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignupController implements AuthApi {

    private final SignupService signupService;
    private final LoginService loginService;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    public SignupController(
        SignupService signupService,
        LoginService loginService,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        this.signupService = signupService;
        this.loginService = loginService;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public ResponseEntity<UserSummary> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(loginService.login(loginRequest, httpServletRequest, httpServletResponse));
    }

    @Override
    public ResponseEntity<UserSummary> signup(SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signupService.signup(signupRequest));
    }
}
