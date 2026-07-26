package com.tickerflow.candle.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record SignalEvent(String symbol, String windowSize, double smaShort, double smaLong, String signalType, Instant timestamp) {
}
