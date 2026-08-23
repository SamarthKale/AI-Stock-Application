package com.stockpredictor.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

private const val UTTERANCE_ID = "market_briefing"

/**
 * Wraps [android.speech.tts.TextToSpeech], handling its async [TextToSpeech.OnInitListener]
 * callback before allowing [speak] calls -- calling speak() before onInit fires is a common
 * source of silently-dropped audio (explicit Phase 5c pitfall to avoid). No network call of any
 * kind: the briefing text is composed entirely by the caller from already-loaded data (see
 * HomeViewModel.buildBriefingText), and TTS synthesis itself is on-device.
 */
class MarketBriefingSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pendingUtterances = mutableListOf<String>()

    init {
        tts = TextToSpeech(appContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts?.language = Locale.getDefault()
                pendingUtterances.forEach { speakInternal(it) }
                pendingUtterances.clear()
            }
        }
    }

    fun speak(text: String) {
        if (isReady) speakInternal(text) else pendingUtterances.add(text)
    }

    private fun speakInternal(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /** Called on screen navigate-away/onDispose so leaving Home mid-briefing doesn't leave audio
     *  playing over the next screen. */
    fun stop() {
        tts?.stop()
        pendingUtterances.clear()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
