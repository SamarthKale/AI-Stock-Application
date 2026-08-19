package com.stockpredictor.backend.user;

import com.stockpredictor.backend.common.dto.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the caller's own profile. The uid always comes from the verified token via
     * SecurityContextHolder (populated by FirebaseAuthenticationFilter) — never from a request
     * parameter, so one user can never read another's profile by guessing an id. By the time this
     * runs, ensureUserExists has already provisioned the row for this request's principal.
     */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        String uid = (String) authentication.getPrincipal();
        // findById (not getReferenceById): open-in-view is disabled, so the entity must be fully
        // loaded within the repository call, not lazily via a proxy accessed after the session closes.
        UserEntity user = userRepository.findById(uid).orElseThrow();
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
