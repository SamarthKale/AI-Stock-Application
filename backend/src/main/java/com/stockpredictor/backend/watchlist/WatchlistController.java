package com.stockpredictor.backend.watchlist;

import com.stockpredictor.backend.common.dto.AddWatchlistItemRequest;
import com.stockpredictor.backend.common.dto.ReorderWatchlistRequest;
import com.stockpredictor.backend.common.dto.WatchlistItemDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistItemDto> getAll(Authentication authentication) {
        return watchlistService.getAll(uid(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistItemDto add(Authentication authentication, @Valid @RequestBody AddWatchlistItemRequest request) {
        return watchlistService.add(uid(authentication), request.symbol());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        watchlistService.delete(uid(authentication), id);
    }

    @PutMapping("/reorder")
    public List<WatchlistItemDto> reorder(Authentication authentication, @Valid @RequestBody ReorderWatchlistRequest request) {
        return watchlistService.reorder(uid(authentication), request.orderedIds());
    }

    /** The current user's id always comes from the verified token (SecurityContextHolder via
     *  FirebaseAuthenticationFilter) — never from a client-supplied parameter. */
    private static String uid(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
