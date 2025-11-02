package pl.kalin.dreamlog.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.common.dto.CreatedResponse;
import pl.kalin.dreamlog.common.security.AuthenticationHelper;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.RegisterRequest;
import pl.kalin.dreamlog.user.dto.SetPasswordRequest;
import pl.kalin.dreamlog.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationHelper authHelper;

    /**
     * Register new user with email and password, then auto-login.
     * GlobalExceptionHandler handles UserAlreadyExistsException.
     */
    @PostMapping("/register")
    public ResponseEntity<CreatedResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        // Create user account
        User user = userService.registerWithPassword(request);

        // Auto-login: authenticate and save session
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(request.email(), request.password());
        Authentication authentication = authenticationManager.authenticate(authToken);

        // Save authentication to session (creates JSESSIONID cookie)
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);

        return ResponseEntity.ok(new CreatedResponse(user.getId()));
    }

    /**
     * Set or change password for authenticated user.
     * Allows OAuth users to add local credentials.
     */
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Boolean>> setPassword(
        @Valid @RequestBody SetPasswordRequest request,
        Authentication authentication
    ) {
        User user = authHelper.getCurrentUser(authentication);
        userService.setPassword(user, request.password());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
