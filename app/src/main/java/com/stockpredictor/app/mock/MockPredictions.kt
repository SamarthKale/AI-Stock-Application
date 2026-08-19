package com.stockpredictor.app.mock

import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection

object MockPredictions {

    // Confidence values (0-100) deliberately span all three PredictionConfidenceBar
    // color bands (<40 coral, 40-70 amber, >70 mint). "symbol" holds the coin id
    // (matches MockCoins.id) so CryptoDetailViewModel/PredictionsViewModel can join on it.
    val all: List<Prediction> = listOf(
        Prediction("bitcoin", 82f, PredictionDirection.Up, 65500.0, "7d", System.currentTimeMillis()),
        Prediction("ethereum", 35f, PredictionDirection.Down, 3250.0, "7d", System.currentTimeMillis()),
        // Null targetPrice: exercises detail screen / confidence bar null-handling.
        Prediction("solana", 58f, PredictionDirection.Flat, null, "7d", System.currentTimeMillis()),
        Prediction("binancecoin", 51f, PredictionDirection.Flat, 580.0, "7d", System.currentTimeMillis()),
        Prediction("ripple", 74f, PredictionDirection.Up, 0.64, "14d", System.currentTimeMillis()),
        Prediction("cardano", 29f, PredictionDirection.Down, 0.35, "14d", System.currentTimeMillis()),
        Prediction("dogecoin", 88f, PredictionDirection.Up, 0.135, "30d", System.currentTimeMillis()),
        Prediction("polkadot", 44f, PredictionDirection.Down, 5.80, "14d", System.currentTimeMillis()),
        Prediction("chainlink", 67f, PredictionDirection.Up, 16.20, "14d", System.currentTimeMillis()),
        Prediction("litecoin", 61f, PredictionDirection.Up, 91.00, "30d", System.currentTimeMillis()),
        Prediction("avalanche-2", 39f, PredictionDirection.Down, 25.40, "14d", System.currentTimeMillis()),
        Prediction("tron", 79f, PredictionDirection.Up, 0.178, "7d", System.currentTimeMillis())
    )

    fun forSymbol(symbol: String): Prediction? = all.firstOrNull { it.symbol == symbol }
}
