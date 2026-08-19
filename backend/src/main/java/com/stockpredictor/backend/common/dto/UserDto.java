package com.stockpredictor.backend.common.dto;

import java.time.Instant;

public record UserDto(String id, String email, String displayName, Instant createdAt) {
}
