package com.stockpredictor.app.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import com.stockpredictor.app.model.PricePoint
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

enum class MomentumTag { BULLISH, BEARISH, NEUTRAL }

data class MomentumResult(val tag: MomentumTag, val confidence: Float)

private const val MODEL_ASSET_NAME = "momentum_model.tflite"

// 7-day return needs price[t] and price[t-7] -- 8 points is the floor for every feature to be
// computable. Mirrors REQUIRED_WARMUP_DAYS-style constants elsewhere in this project (Phase 5's
// xgboost_model.py), just far smaller since this model only uses 3 plain-return features.
private const val MIN_HISTORY_POINTS = 8

/**
 * Runs the Phase 5b on-device momentum model entirely locally -- no network call, works in
 * airplane mode by construction (the .tflite file ships inside the APK as an asset). Deliberately
 * distinct from Phase 5's server-side XGBoost prediction: this is a lightweight, instant,
 * offline-only signal shown *alongside* (never replacing) the AI Prediction card.
 *
 * Honest caveat (see ai-service/artifacts/momentum_training_report.json, produced by
 * training/train_momentum_tflite.py): validation showed only a marginal edge over a naive
 * majority-class baseline (macro F1 0.30 vs 0.25) and 0% recall on UP moves -- CryptoDetailScreen
 * must present this as a weak, best-effort local signal, not a reliable trading indicator.
 *
 * The 3 input features (1-day/3-day/7-day % return) are plain arithmetic computed here from
 * already-cached [PricePoint] history -- deliberately NOT "feature engineering": the model's only
 * real logic lives in the trained TFLite graph built in Python
 * (ai-service/training/train_momentum_tflite.py), per Tech Stack's rule that Java/Kotlin never
 * contains model logic.
 *
 * Singleton (matches [com.stockpredictor.app.data.repository.CoinRepository]'s getInstance
 * pattern) so the Interpreter is loaded once for the app's process lifetime, not once per
 * CryptoDetailViewModel (which is recreated on every navigation to a coin's detail screen).
 */
class OnDeviceMomentumClassifier private constructor(context: Context) {

    private val appContext = context.applicationContext

    // Lazy: the model file is only memory-mapped and the Interpreter only constructed on first
    // use, but from then on this exact same Interpreter instance serves every classify() call --
    // never reloaded per call.
    private val interpreter: Interpreter by lazy { Interpreter(loadModelFile()) }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor: AssetFileDescriptor = appContext.assets.openFd(MODEL_ASSET_NAME)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength,
            )
        }
    }

    /** Null if there isn't enough cached history yet to compute all 3 features -- callers treat
     *  this the same way Crypto Detail already treats a null server prediction (a small, scoped
     *  "not available" state, never a whole-screen error). */
    suspend fun classify(history: List<PricePoint>): MomentumResult? = withContext(Dispatchers.Default) {
        val sorted = history.sortedBy { it.timestamp }
        if (sorted.size < MIN_HISTORY_POINTS) return@withContext null

        val latest = sorted.last().price
        val ret1d = returnPct(latest, sorted[sorted.size - 2].price)
        val ret3d = returnPct(latest, sorted[sorted.size - 4].price)
        val ret7d = returnPct(latest, sorted[sorted.size - 8].price)

        val input = arrayOf(floatArrayOf(ret1d, ret3d, ret7d))
        // Output order [DOWN, FLAT, UP] -- matches LABELS in train_momentum_tflite.py exactly;
        // changing that script's label order without updating this mapping would silently swap
        // Bullish/Bearish.
        val output = Array(1) { FloatArray(3) }
        interpreter.run(input, output)

        val probs = output[0]
        val bestIndex = probs.indices.maxByOrNull { probs[it] } ?: 1
        val tag = when (bestIndex) {
            0 -> MomentumTag.BEARISH
            2 -> MomentumTag.BULLISH
            else -> MomentumTag.NEUTRAL
        }
        MomentumResult(tag = tag, confidence = probs[bestIndex] * 100f)
    }

    private fun returnPct(current: Double, past: Double): Float {
        if (past == 0.0) return 0f
        return ((current - past) / past * 100.0).toFloat()
    }

    companion object {
        @Volatile private var instance: OnDeviceMomentumClassifier? = null

        fun getInstance(context: Context): OnDeviceMomentumClassifier =
            instance ?: synchronized(this) {
                instance ?: OnDeviceMomentumClassifier(context.applicationContext).also { instance = it }
            }
    }
}
