package com.stockpredictor.app.model

sealed class PredictionDirection {
    data object Up : PredictionDirection()
    data object Down : PredictionDirection()
    data object Flat : PredictionDirection()
}

/**
 * [confidence] is expressed on a 0-100 scale (not 0-1) so it maps directly
 * onto [com.stockpredictor.app.ui.components.PredictionConfidenceBar].
 */
data class Prediction(
    val symbol: String,
    val confidence: Float,
    val direction: PredictionDirection,
    val targetPrice: Double?,
    val horizon: String,
    val generatedAt: Long
)
