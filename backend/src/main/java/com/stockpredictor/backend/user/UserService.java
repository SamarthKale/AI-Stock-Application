package com.stockpredictor.backend.user;

import com.stockpredictor.backend.config.FirebaseUserPrincipal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * There is no separate "register" endpoint — a user's row is lazily provisioned the first time
 * their verified token is seen (called from FirebaseAuthenticationFilter on every request), so
 * the watchlist/portfolio tables always have a valid user_id to reference.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity ensureUserExists(FirebaseUserPrincipal principal) {
        return userRepository.findById(principal.uid())
                .map(existing -> refreshIfChanged(existing, principal))
                .orElseGet(() -> userRepository.save(new UserEntity(
                        principal.uid(),
                        principal.email(),
                        principal.displayName(),
                        Instant.now())));
    }

    private UserEntity refreshIfChanged(UserEntity existing, FirebaseUserPrincipal principal) {
        boolean emailChanged = principal.email() != null && !principal.email().equals(existing.getEmail());
        boolean nameChanged = principal.displayName() != null && !principal.displayName().equals(existing.getDisplayName());
        if (emailChanged || nameChanged) {
            if (emailChanged) existing.setEmail(principal.email());
            if (nameChanged) existing.setDisplayName(principal.displayName());
            return userRepository.save(existing);
        }
        return existing;
    }
}
