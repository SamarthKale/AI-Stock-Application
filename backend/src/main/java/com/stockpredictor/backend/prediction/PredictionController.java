package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.common.dto.PredictionHistoryRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Predictions are coin-scoped, not user-scoped (every user sees the same prediction for a
 * given coin) — still sits under the standard Firebase-token-verified {@code anyRequest()
 * .authenticated()} catch-all like every other endpoint (no SecurityConfig change needed,
 * same as watchlist/portfolio required none), for consistency rather than technical necessity.
 * The verified uid is used only as {@link PredictionRateLimiter}'s per-user key (Phase 6), same
 * pattern as ChatbotController — never to scope any stored prediction data.
 */
@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/{coinId}")
    public PredictionResponseDto predict(
            Authentication authentication,
            @PathVariable String coinId,
            @Valid @RequestBody PredictionHistoryRequestDto request) {
        return predictionService.getPrediction(uid(authentication), coinId, request);
    }

    private static String uid(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
