package com.stockpredictor.backend.alerts;

import java.util.Map;
import java.util.Set;

/** Seam over "where current price/24h-change data comes from" — mirrors PredictionClient's
 *  interface+impl pattern. This is the backend's first-ever market-data integration (Phase 4
 *  deliberately kept the backend market-data-free; Phase 5c adds this specifically because a
 *  server-scheduled alert job has no other way to know current prices — see the Phase 5c plan). */
public interface MarketDataClient {

    record CoinMarketSnapshot(String coinId, String name, Double currentPrice, Double priceChangePercentage24h) {
    }

    /** One batched call for every id in [coinIds] — never one call per coin.
     *  @throws AlertDataUnavailableException if the request fails. */
    Map<String, CoinMarketSnapshot> getMarketSnapshots(Set<String> coinIds);
}
