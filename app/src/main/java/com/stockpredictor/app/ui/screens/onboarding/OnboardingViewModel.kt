package com.stockpredictor.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingSlide(val title: String, val body: String)

private val defaultSlides = listOf(
    OnboardingSlide(
        title = "Track every market",
        body = "Watch NSE, BSE, and global tickers side by side, updated the moment you open the app.",
    ),
    OnboardingSlide(
        title = "AI-driven predictions",
        body = "See confidence-scored price direction predictions, powered by machine learning.",
    ),
    OnboardingSlide(
        title = "Your portfolio, always in view",
        body = "Track holdings, gains, and losses at a glance with a calm, clutter-free layout.",
    ),
)

/**
 * Onboarding is static local content, not a fetch — so its "ui state" is a plain
 * slide list rather than the Loading/Empty/Error UiState<T> used by data screens.
 * "Don't show again" persistence is out of scope until Phase 2's SettingsDao.
 */
class OnboardingViewModel : ViewModel() {
    private val _slides = MutableStateFlow(defaultSlides)
    val slides: StateFlow<List<OnboardingSlide>> = _slides.asStateFlow()
}
