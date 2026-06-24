package com.promptvault.api.currentuser;

import com.promptvault.contract.api.CurrentUserApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController implements CurrentUserApi {

    @Override
    public ResponseEntity<Void> getCurrentUser() {
        return ResponseEntity.noContent().build();
    }
}
