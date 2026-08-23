package com.stockpredictor.backend.alerts;

import java.util.Map;

/** Seam over "how a push actually gets sent" — mirrors ChatbotClient's interface+impl pattern. */
public interface PushSender {

    /** Data-payload-only (never a {@code notification} block) — see the Phase 5c plan §6 for why:
     *  this makes StockPredictorFcmService.onMessageReceived fire uniformly whether the app is
     *  foregrounded, backgrounded, or killed, with one code path building the deep-linking
     *  notification instead of two.
     *  @throws AlertDataUnavailableException if the send fails. */
    void sendDataMessage(String fcmToken, Map<String, String> data);
}
