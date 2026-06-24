package com.promptvault.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CorsSecurityTest.ProbeController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class })
@TestPropertySource(properties = "prompt-vault.cors.allowed-origins=http://localhost:3000")
class CorsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsConfiguredBrowserOriginToPreflightProtectedApiRoutes() throws Exception {
        mockMvc.perform(options("/api/user")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/user")
        ResponseEntity<Void> currentUser() {
            return ResponseEntity.noContent().build();
        }
    }
}
