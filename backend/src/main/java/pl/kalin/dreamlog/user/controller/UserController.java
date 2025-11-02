package pl.kalin.dreamlog.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.common.security.AuthenticationHelper;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationHelper authHelper;

    /**
     * Get current authenticated user information..
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = authHelper.getCurrentUser(authentication);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
