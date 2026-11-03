package com.stockpredictor.app.data.learn

import androidx.annotation.RawRes
import com.stockpredictor.app.R

enum class LearnType { VIDEO, PDF }
enum class LearnCategory { TUTORIAL, DEMO }

/**
 * One bundled piece of learning content.
 *
 * To add your own file: drop an .mp4 into `app/src/main/res/raw/` (lowercase, underscores, e.g.
 * `how_to_buy_btc.mp4` -> `R.raw.how_to_buy_btc`) or a .pdf into `app/src/main/assets/learn/`, then
 * add one [LearnItem] to [LearnContent.all] below. Nothing else needs to change.
 *
 * Videos are referenced via `R.raw.*` (not a string name) on purpose: the release build's resource
 * shrinker only keeps resources it can see referenced in code.
 */
data class LearnItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: LearnType,
    val category: LearnCategory,
    val durationLabel: String,
    @RawRes val rawRes: Int? = null,
    val assetPath: String? = null,
)

object LearnContent {
    val all: List<LearnItem> = listOf(
        // ---- Tutorials: crypto trading & how to use crypto -------------------------------------
        LearnItem(
            id = "how_to_predict_crypto_markets",
            title = "How to Predict the Crypto Markets",
            subtitle = "A beginner's guide to reading and forecasting crypto prices",
            type = LearnType.VIDEO,
            category = LearnCategory.TUTORIAL,
            durationLabel = "6:34",
            rawRes = R.raw.how_to_predict_crypto_markets,
        ),
        LearnItem(
            id = "why_smart_money_buying_bitcoin",
            title = "Why Is Smart Money Buying Bitcoin?",
            subtitle = "Why institutions are adopting Bitcoin",
            type = LearnType.VIDEO,
            category = LearnCategory.TUTORIAL,
            durationLabel = "9:22",
            rawRes = R.raw.why_smart_money_buying_bitcoin,
        ),
        LearnItem(
            id = "crypto_basics_pdf",
            title = "Crypto Basics",
            subtitle = "What crypto is, how to read the market and use a wallet safely",
            type = LearnType.PDF,
            category = LearnCategory.TUTORIAL,
            durationLabel = "3 pages",
            assetPath = "learn/crypto_basics_sample.pdf",
        ),
        // ---- Demo: app walkthrough ---------------------------------------------------------------
        LearnItem(
            id = "app_tour_video",
            title = "App Tour",
            subtitle = "A quick walkthrough of the redesigned app",
            type = LearnType.VIDEO,
            category = LearnCategory.DEMO,
            durationLabel = "0:34",
            rawRes = R.raw.demo_app_tour,
        ),
    )

    fun byId(id: String): LearnItem? = all.firstOrNull { it.id == id }
    fun byCategory(category: LearnCategory): List<LearnItem> = all.filter { it.category == category }
}
