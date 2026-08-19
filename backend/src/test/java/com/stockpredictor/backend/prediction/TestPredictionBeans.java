package com.stockpredictor.backend.prediction;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestPredictionBeans {

    @Bean
    @Primary
    public PredictionClient predictionClient() {
        return new FakePredictionClient();
    }
}
