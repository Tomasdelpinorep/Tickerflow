package com.tickerflow.notification.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TradeEvent(Long id, String symbol, String signalType, int quantity, BigDecimal price, String status,
                         Instant createdAt) {
}
