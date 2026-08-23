package com.stockpredictor.backend.alerts;

import java.util.List;

/** Test double avoiding the need for a live Firestore project during backend tests — same
 *  reasoning as FakePredictionClient/FakeChatbotClient avoiding live external services. */
public class FakeWatchlistReader implements WatchlistReader {

    private List<UserWatchlist> watchlists = List.of();
    private boolean shouldThrow = false;

    public void setWatchlists(List<UserWatchlist> watchlists) {
        this.watchlists = watchlists;
    }

    public void setShouldThrow(boolean shouldThrow) {
        this.shouldThrow = shouldThrow;
    }

    @Override
    public List<UserWatchlist> getAllUserWatchlists() {
        if (shouldThrow) {
            throw new AlertDataUnavailableException("Simulated Firestore failure", null);
        }
        return watchlists;
    }
}
