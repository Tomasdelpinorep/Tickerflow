package com.tickerflow.trading.events;

import lombok.Builder;

import java.time.Instant;

@Builder
public record SignalEvent(String symbol, String windowSize, String signalType, double smaShort, double smaLong,
                          Instant timestamp) {
}
