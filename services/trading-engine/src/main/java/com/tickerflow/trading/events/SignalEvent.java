package com.tickerflow.trading.events;

import com.tickerflow.trading.enums.SignalType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SignalEvent(String symbol, String windowSize, SignalType signalType, BigDecimal executePrice,
                          double smaShort, double smaLong, Instant timestamp) {
}
