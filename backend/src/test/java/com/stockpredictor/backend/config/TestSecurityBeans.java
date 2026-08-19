package com.stockpredictor.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSecurityBeans {

    @Bean
    @Primary
    public FirebaseTokenVerifier firebaseTokenVerifier() {
        return new FakeFirebaseTokenVerifier();
    }
}
