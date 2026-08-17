package com.stockpredictor.app.mock

import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection

object MockPredictions {

    // Confidence values (0-100) deliberately span all three PredictionConfidenceBar
    // color bands (<40 coral, 40-70 amber, >70 mint).
    val all: List<Prediction> = listOf(
        Prediction("RELIANCE.NS", 82f, PredictionDirection.Up, 3050.0, "7d", System.currentTimeMillis()),
        Prediction("TCS.NS", 35f, PredictionDirection.Down, 3720.0, "7d", System.currentTimeMillis()),
        // Null targetPrice: exercises detail screen / confidence bar null-handling.
        Prediction("INFY.NS", 58f, PredictionDirection.Flat, null, "7d", System.currentTimeMillis()),
        Prediction("HDFCBANK.NS", 51f, PredictionDirection.Flat, 1645.0, "7d", System.currentTimeMillis()),
        Prediction("TATASTEEL.BO", 74f, PredictionDirection.Up, 158.0, "14d", System.currentTimeMillis()),
        Prediction("WIPRO.BO", 29f, PredictionDirection.Down, 462.0, "14d", System.currentTimeMillis()),
        Prediction("ICICIBANK.NS", 88f, PredictionDirection.Up, 1240.0, "30d", System.currentTimeMillis()),
        Prediction("BAJFINANCE.NS", 44f, PredictionDirection.Down, 6980.0, "14d", System.currentTimeMillis()),
        Prediction("SBIN.BO", 67f, PredictionDirection.Up, 845.0, "14d", System.currentTimeMillis()),
        Prediction("AAPL", 61f, PredictionDirection.Up, 236.0, "30d", System.currentTimeMillis()),
        Prediction("MSFT", 39f, PredictionDirection.Down, 402.0, "14d", System.currentTimeMillis()),
        Prediction("ADANIENT.NS", 79f, PredictionDirection.Up, 2540.0, "7d", System.currentTimeMillis())
    )

    fun forSymbol(symbol: String): Prediction? = all.firstOrNull { it.symbol == symbol }
}
