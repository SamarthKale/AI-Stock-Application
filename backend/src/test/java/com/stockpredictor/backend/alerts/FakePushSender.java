package com.stockpredictor.backend.alerts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FakePushSender implements PushSender {

    public record SentMessage(String fcmToken, Map<String, String> data) {
    }

    private final List<SentMessage> sent = new ArrayList<>();
    private boolean shouldThrow = false;

    public void setShouldThrow(boolean shouldThrow) {
        this.shouldThrow = shouldThrow;
    }

    public List<SentMessage> getSent() {
        return sent;
    }

    @Override
    public void sendDataMessage(String fcmToken, Map<String, String> data) {
        if (shouldThrow) {
            throw new AlertDataUnavailableException("Simulated FCM failure", null);
        }
        sent.add(new SentMessage(fcmToken, data));
    }
}
