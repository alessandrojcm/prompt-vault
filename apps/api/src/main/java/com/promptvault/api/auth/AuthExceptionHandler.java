package com.promptvault.api.auth;

import com.promptvault.contract.model.AuthenticationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<AuthenticationErrorResponse> handleInvalidCredentialsException() {
        AuthenticationErrorResponse response = new AuthenticationErrorResponse();
        response.setMessage("Invalid username or password.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DisabledAccountException.class)
    ResponseEntity<AuthenticationErrorResponse> handleDisabledAccountException() {
        AuthenticationErrorResponse response = new AuthenticationErrorResponse();
        response.setMessage("Your account is disabled. Contact an administrator.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
