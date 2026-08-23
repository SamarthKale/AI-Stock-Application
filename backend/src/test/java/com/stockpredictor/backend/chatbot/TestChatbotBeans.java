package com.stockpredictor.backend.chatbot;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestChatbotBeans {

    @Bean
    @Primary
    public ChatbotClient chatbotClient() {
        return new FakeChatbotClient();
    }
}
