package com.stockpredictor.backend.alerts;

import java.util.List;
import java.util.Set;

/** Seam over "where per-user watchlist + FCM token data comes from" — mirrors the
 *  PredictionClient/ChatbotClient interface+impl pattern so tests can substitute a fake without a
 *  live Firestore project. */
public interface WatchlistReader {

    record UserWatchlist(String uid, Set<String> coinIds, String fcmToken) {
    }

    /** @throws AlertDataUnavailableException if Firestore is unreachable or errors. Users with no
     *  fcmToken are excluded — there would be nowhere to send a push. */
    List<UserWatchlist> getAllUserWatchlists();
}
