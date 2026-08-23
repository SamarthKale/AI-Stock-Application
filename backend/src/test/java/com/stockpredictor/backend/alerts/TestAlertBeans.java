package com.stockpredictor.backend.alerts;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestAlertBeans {

    @Bean
    @Primary
    public WatchlistReader watchlistReader() {
        return new FakeWatchlistReader();
    }

    @Bean
    @Primary
    public MarketDataClient marketDataClient() {
        return new FakeMarketDataClient();
    }

    @Bean
    @Primary
    public PushSender pushSender() {
        return new FakePushSender();
    }
}
