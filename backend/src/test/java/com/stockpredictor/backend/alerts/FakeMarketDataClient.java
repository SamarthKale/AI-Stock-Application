package com.stockpredictor.backend.alerts;

import java.util.Map;
import java.util.Set;

public class FakeMarketDataClient implements MarketDataClient {

    private Map<String, CoinMarketSnapshot> snapshots = Map.of();
    private boolean shouldThrow = false;

    public void setSnapshots(Map<String, CoinMarketSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public void setShouldThrow(boolean shouldThrow) {
        this.shouldThrow = shouldThrow;
    }

    @Override
    public Map<String, CoinMarketSnapshot> getMarketSnapshots(Set<String> coinIds) {
        if (shouldThrow) {
            throw new AlertDataUnavailableException("Simulated CoinGecko failure", null);
        }
        return snapshots;
    }
}
